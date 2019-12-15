/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.ContainerContext
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
    internal val containerContext: ContainerContext,
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
val Container<*>.profiles: List<String> get() = containerContext.profiles

fun <M : Module> Container.Companion.start(
    namespace: String = "",
    propertyFilename: String = "application",
    lazy: Boolean = false,
    propertySource: (List<String>) -> PropertySource = defaultPropertySource(propertyFilename),
    module: () -> M
): Container<M> {
    contract {
        callsInPlace(module, InvocationKind.EXACTLY_ONCE)
    }
    val startedMs = System.currentTimeMillis()
    logger.info("Starting container")
    val context = ContainerContext.create(lazy, propertySource)
    val rootModule = Module.create(context, namespace, module)
    context.start()
    return Container(context, rootModule).apply {
        Runtime.getRuntime().addShutdownHook(Thread {
            destroy()
        })
        logger.info("Started container in ${System.currentTimeMillis() - startedMs}ms")
    }
}

/**
 * Retrieves instance [T] of the specified [binding].
 * ```kotlin
 * val someService = container.get { someService }
 * ```
 */
fun <M : Module, T : Any> Container<M>.get(binding: M.() -> Binding<T>): T {
    contract {
        callsInPlace(binding, InvocationKind.EXACTLY_ONCE)
    }
    return module.binding().provider().instance()
}

/**
 * Destroys this Container.
 */
fun <M : Module> Container<M>.destroy() {
    containerContext.destroy()
}

fun defaultPropertySource(propertyFilename: String): (List<String>) -> PropertySource =
    { profiles -> loadPropertyFilesFromClasspath(propertyFilename, profiles) }
