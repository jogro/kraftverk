/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.*
import mu.KotlinLogging
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Provides access to [Property] and [Bean] instances
 * in a specified implementation [M] of [Module].
 *
 * Given a module:
 * ```kotlin
 * class AppModule : Module() {
 *     val username by stringProperty()
 *     val someService by bean {
 *         SomeService(
 *             username()
 *         )
 *     }
 * }
 * ```
 * Call the [App.Companion.start] method to construct an App
 * instance.
 * ```kotlin
 * val app = App.start { AppModule() }
 * ```
 * To access the bean instance, call the [App.get]
 * method:
 * ```kotlin
 * val someService = app.get { someService }
 * ```
 */
class App<M : Module> internal constructor(
    internal val appContext: AppContext,
    internal val module: M
) {
    companion object
}

/**
 * Contains the names of the active profiles set at
 * application startup.
 *
 * To set the active profiles, you can use
 * - an environment variable named 'KRAFTVERK_ACTIVE_PROFILES'
 * - a system property named 'kraftverk.active.profiles'
 *
 * The value provided should be a comma delimited string
 * containing the names of the profiles.
 *
 * You can also call [Module.useProfiles].
 *
 */
val App<*>.profiles: List<String> get() = appContext.profiles

/**
 * Retrieves the value of the specified [component].
 * ```kotlin
 * val username = app.get { username }
 * ```
 */
fun <M : Module, T : Any> App<M>.get(component: M.() -> Component<T>): T {
    contract {
        callsInPlace(component, InvocationKind.EXACTLY_ONCE)
    }
    return module.component().provider().instance()
}

/**
 * Destroys this App.
 */
fun <M : Module> App<M>.destroy() {
    appContext.destroy()
}

private val logger = KotlinLogging.logger {}

fun <M : Module> App.Companion.start(
    namespace: String = "",
    propertyFilename: String = "application",
    lazy: Boolean = false,
    propertyReader: (List<String>) -> (String) -> String? = defaultPropertyReader(propertyFilename),
    module: () -> M
): App<M> {
    contract {
        callsInPlace(module, InvocationKind.EXACTLY_ONCE)
    }
    val started = System.currentTimeMillis()
    logger.info("Starting app")
    val appContext = AppContext(lazyBeans = lazy, lazyProps = false, propertyReader = propertyReader)
    val rootModule = ModuleContext.use(appContext, namespace) { module() }
    with(appContext) {
        prepare()
        start()
    }
    return App(appContext, rootModule).also {
        Runtime.getRuntime().addShutdownHook(Thread {
            it.destroy()
        })
        logger.info("Started app in ${System.currentTimeMillis() - started}ms ")
    }
}

private fun defaultPropertyReader(propertyFilename: String): (List<String>) -> (String) -> String? =
    { profiles -> loadPropertyFilesFromClasspath(propertyFilename, profiles)::get }
