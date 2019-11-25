/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Provides access to [Property] and [Bean] instances
 * in a specified implementation [M] of [Module].
 *
 * Given a module:
 * ```kotlin
 * class AppModule : Module() {
 *     val username by property()
 *     val someService by bean {
 *         SomeService(
 *             username()
 *         )
 *     }
 * }
 * ```
 * Call the [App.start] method to construct an App
 * instance.
 * ```kotlin
 * val app = App.start(::AppModule)
 * ```
 * To access the bean instance, call the [App.getBean]
 * method:
 * ```kotlin
 * val someService = app.getBean { someService }
 * ```
 */
class App<M : Module> internal constructor(
    internal val appContext: AppContext,
    internal val module: M
) {

    /**
     * Contains the names of the active profiles set at
     * application startup.
     *
     * To set the active profiles, you can use
     * - An environment variable named 'KRAFTVERK_ACTIVE_PROFILES'
     * - A system property named 'kraftverk.active.profiles'
     * The value provided should be a comma delimited string
     * containing the names of the profiles.

     * You can also call [Module.useProfiles].
     *
     */
    val profiles: List<String> = appContext.profiles

    companion object

}

/**
 * Retrieves an instance [T] of the specified [bean].
 * ```kotlin
 * val someService = app.getBean { someService }
 * ```
 */
fun <M : Module, T : Any> App<M>.getBean(bean: M.() -> Bean<T>): T {
    contract {
        callsInPlace(bean, InvocationKind.EXACTLY_ONCE)
    }
    return module.bean().provider().get()
}

/**
 * Retrieves the value of the specified [property].
 * ```kotlin
 * val username = app.getProperty { username }
 * ```
 */
fun <M : Module, T : Any> App<M>.getProperty(property: M.() -> Property<T>): T {
    contract {
        callsInPlace(property, InvocationKind.EXACTLY_ONCE)
    }
    return module.property().provider().get()
}

/**
 * Destroys this App.
 */
fun <M : Module> App<M>.destroy() {
    appContext.destroy()
}

/**
 * Constructs an App instance given an implementation [M]
 * of [Module].
 * ```kotlin
 * val app = App.start(::AppModule)
 * ```
 * Detection of missing properties is fail fast.
 * All beans are eagerly instantiated.
 */
fun <M : Module> App.Companion.start(
    module: () -> M
): App<M> {
    contract {
        callsInPlace(module, InvocationKind.EXACTLY_ONCE)
    }
    return startCustom(
        module = module
    )
}

/**
 * Constructs an App instance given an
 * implementation [M] of [Module].
 * ```kotlin
 * val app = App.start(::AppModule) {
 *     bind(username) to { "guest" }
 * }
 * ```
 * Detection of missing properties is fail fast.
 * All beans are eagerly instantiated.
 */
fun <M : Module> App.Companion.start(
    module: () -> M,
    configure: M.() -> Unit
): App<M> {
    contract {
        callsInPlace(module, InvocationKind.EXACTLY_ONCE)
        callsInPlace(configure, InvocationKind.EXACTLY_ONCE)
    }
    return startCustom(
        module = module,
        configure = configure
    )
}

/**
 * Constructs an App instance given an implementation [M]
 * of [Module].
 * ```kotlin
 * val app = App.startLazy(::AppModule)
 * ```
 * Detection of missing properties is fail fast.
 * All beans are lazily instantiated.
 */
fun <M : Module> App.Companion.startLazy(
    module: () -> M
): App<M> {
    contract {
        callsInPlace(module, InvocationKind.EXACTLY_ONCE)
    }
    return startCustom(
        module = module,
        defaultLazyBeans = true
    )
}

/**
 * Constructs an App instance given an
 * implementation [M] of [Module].
 * ```kotlin
 * val app = App.startLazy(::AppModule) {
 *     bind(username) to { "guest" }
 * }
 * ```
 * Detection of missing properties is fail fast.
 * All beans are lazily instantiated.
 */
fun <M : Module> App.Companion.startLazy(
    module: () -> M,
    configure: M.() -> Unit
): App<M> {
    contract {
        callsInPlace(module, InvocationKind.EXACTLY_ONCE)
        callsInPlace(configure, InvocationKind.EXACTLY_ONCE)
    }
    return startCustom(
        module = module,
        defaultLazyBeans = true,
        configure = configure
    )
}

fun <M : Module> App.Companion.startCustom(
    module: () -> M,
    namespace: String = "",
    defaultLazyBeans: Boolean = false,
    defaultLazyProps: Boolean = false,
    propertyReader: (List<String>) -> (String) -> String? = ::defaultPropertyReader,
    configure: M.() -> Unit = {}
): App<M> {
    contract {
        callsInPlace(module, InvocationKind.EXACTLY_ONCE)
        callsInPlace(configure, InvocationKind.EXACTLY_ONCE)
    }
    val appContext = AppContext(defaultLazyBeans, defaultLazyProps, propertyReader)
    val rootModule = ModuleContext.use(appContext, namespace) { module().apply(configure) }
    with(appContext) {
        initialize()
        start()
    }
    return App(appContext, rootModule).also {
        Runtime.getRuntime().addShutdownHook(Thread {
            it.destroy()
        })
    }
}

private fun defaultPropertyReader(profiles: List<String>): (String) -> String? =
    loadPropertyFilesFromClasspath("application", profiles)::get
