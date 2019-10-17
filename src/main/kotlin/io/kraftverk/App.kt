/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.AppContext
import io.kraftverk.internal.ModuleContext
import io.kraftverk.internal.loadPropertyFilesFromClasspath
import io.kraftverk.internal.provider

/**
 * Provides access to [Property] and [Bean] instances
 * in a specified implementation [M] of [Module].
 *
 * Given a module:
 * ```kotlin
 * class AppModule : Module() {
 *     val username by property()
 *     val someService by singleton {
 *         SomeService(
 *             username()
 *         )
 *     }
 * }
 * ```
 * Call the [App.start] method to construct an App
 * instance.
 * ```kotlin
 * val app = App.start(::AppModule) {
 * ```
 * To access the bean instance, use the [App.getBean]
 * method:
 * ```kotlin
 * val someService = app.getBean { someService }
 * ```
 */
class App<M : Module> private constructor(
    private val appContext: AppContext,
    private val module: M
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

    /**
     * Retrieves an instance [T] of the specified [bean].
     * ```kotlin
     * val someService = app.getBean { someService }
     * ```
     */
    fun <T : Any> getBean(bean: M.() -> Bean<T>): T =
        module.bean().provider().get()

    /**
     * Retrieves the value of the specified [property].
     * ```kotlin
     * val username = app.getProperty { username }
     * ```
     */
    fun getProperty(property: M.() -> Property): String =
        module.property().provider().get()

    /**
     * Destroys this App.
     */
    fun destroy() {
        appContext.destroy()
    }

    companion object {

        /**
         * Constructs an App instance given an implementation [M]
         * of [Module].
         * ```kotlin
         * val app = App.start(::AppModule)
         * ```
         * Detection of missing properties is fail fast.
         * All beans are eagerly instantiated.
         */
        fun <M : Module> start(
            module: () -> M
        ): App<M> = startCustomized(
            module = module
        )

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
        fun <M : Module> start(
            module: () -> M,
            configure: M.() -> Unit
        ): App<M> = startCustomized(
            module = module,
            configure = configure
        )

        /**
         * Constructs an App instance given an implementation [M]
         * of [Module].
         * ```kotlin
         * val app = App.startLazy(::AppModule)
         * ```
         * Detection of missing properties is fail fast.
         * All beans are lazily instantiated.
         */
        fun <M : Module> startLazy(
            module: () -> M
        ): App<M> =
            startCustomized(
                module = module,
                defaultLazyBeans = true
            )

        /**
         * Constructs an App instance given an
         * implementation [M] of [Module].
         * ```kotlin
         * val app = App.startLazy(::AppModule) {
         *     bind(username) with { "guest" }
         * }
         * ```
         * Detection of missing properties is fail fast.
         * All beans are lazily instantiated.
         */
        fun <M : Module> startLazy(
            module: () -> M,
            configure: M.() -> Unit
        ): App<M> =
            startCustomized(
                module = module,
                defaultLazyBeans = true,
                configure = configure
            )


        fun <M : Module> startCustomized(
            module: () -> M,
            namespace: String = "",
            defaultLazyBeans: Boolean = false,
            defaultLazyProps: Boolean = false,
            propertyReader: (List<String>) -> (String) -> String? = Companion::defaultPropertyReader,
            configure: M.() -> Unit = {}
        ): App<M> {
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

    }

}
