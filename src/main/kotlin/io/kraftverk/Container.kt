/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.Registry
import io.kraftverk.internal.loadPropertyFilesFromClasspath
import io.kraftverk.internal.provider
import mu.KotlinLogging
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

private val logger = KotlinLogging.logger {}

/**
 * Provides access to [Property] and [Bean] instances in a specified
 * implementation [M] of [Module].
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
 * Call the [Container.Companion.start] method to start a Container
 * instance.
 * ```kotlin
 * val container = Container.start { AppModule() }
 * ```
 * To access the bean instance, call the [Container.get]
 * method:
 * ```kotlin
 * val someService = container.get { someService }
 * ```
 */
class Container<M : Module> internal constructor(
    internal val registry: Registry,
    internal val module: M
) {
    companion object
}

/**
 * Contains the active profiles set at startup.
 *
 * To set the active profiles, you can use
 * - an environment variable named 'KRAFTVERK_ACTIVE_PROFILES'
 * - a system property named 'kraftverk.active.profiles'
 *
 * The value provided should be a comma delimited string
 * containing the names of the profiles.
 *
 */
val Container<*>.profiles: List<String> get() = registry.profiles

fun <M : Module> Container.Companion.start(
    namespace: String = "",
    propertyFilename: String = "application",
    lazy: Boolean = false,
    propertyReader: (List<String>) -> (String) -> String? = defaultPropertyReader(propertyFilename),
    module: () -> M
): Container<M> {
    contract {
        callsInPlace(module, InvocationKind.EXACTLY_ONCE)
    }
    val started = System.currentTimeMillis()
    logger.info("Starting container")
    val registry = Registry(lazyBeans = lazy, lazyProps = lazy, propertyReader = propertyReader)
    val rootModule = Module.use(registry, namespace) { module() }
    registry.start()
    return Container(registry, rootModule).also {
        Runtime.getRuntime().addShutdownHook(Thread {
            it.destroy()
        })
        logger.info("Started container in ${System.currentTimeMillis() - started}ms ")
    }
}

/**
 * Retrieves the value of the specified [component].
 * ```kotlin
 * val username = container.get { username }
 * ```
 */
fun <M : Module, T : Any> Container<M>.get(component: M.() -> Component<T>): T {
    contract {
        callsInPlace(component, InvocationKind.EXACTLY_ONCE)
    }
    return module.component().provider().instance()
}

/**
 * Destroys this Container.
 */
fun <M : Module> Container<M>.destroy() {
    registry.destroy()
}

private fun defaultPropertyReader(propertyFilename: String): (List<String>) -> (String) -> String? =
    { profiles -> loadPropertyFilesFromClasspath(propertyFilename, profiles)::get }
