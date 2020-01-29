/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import mu.KotlinLogging
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.time.measureTimedValue

/**
 * Bootstrap
 */
class Kraftverk {
    companion object
}

private val logger = KotlinLogging.logger {}

/**
 * A factory function that creates a [Managed] instance of the specified implementation
 * [M] of [Module].
 * ```kotlin
 * val app: Managed<AppModule> = Kraftverk.manage { AppModule() }
 * ```
 * The function will by default perform the following actions:
 * 1) All [Value] bindings declared in the [Module] are eagerly looked up using the supplied [Environment].
 * Should any value be missing an exception is thrown.
 * 2) All [Bean] bindings are eagerly instantiated.
 *
 * Call the [Managed.destroy] method to destroy the [Managed] instance. Otherwise, shutdown will be performed
 * by a shutdown hook.
 */
fun <M : Module> Kraftverk.Companion.manage(
    namespace: String = "",
    lazy: Boolean = false,
    refreshable: Boolean = true,
    environment: Environment = Environment.standard(),
    module: () -> M
): Managed<M> {
    contract {
        callsInPlace(module, InvocationKind.EXACTLY_ONCE)
    }
    return measureTimedValue {
        logger.info("Creating managed module")
        val container = Container(lazy, refreshable, environment)
        val rootModule = ModuleCreationContext.use(container, namespace) { module() }
        container.start()
        Managed(container, rootModule).apply {
            Runtime.getRuntime().addShutdownHook(Thread {
                destroy()
            })
        }
    }.also {
        logger.info("Created managed module in ${it.duration}")
    }.value
}

/**
 * Provides access to and manages [Bean] and [Value] instances in a specified
 * implementation [M] of [Module].
 *
 * Given a module:
 * ```kotlin
 * class AppModule : Module() {
 *     val username by string()   //<-- Value binding
 *     val someService by bean {  //<-- Bean binding
 *         SomeService(
 *             username()
 *         )
 *     }
 * }
 * ```
 * Call the [Kraftverk.Companion.manage] method to create a [Managed] instance of the module.
 * ```kotlin
 * val app: Managed<AppModule> = Kraftverk.manage { AppModule() }
 * ```
 */
class Managed<M : Module> internal constructor(
    internal val container: Container,
    internal val module: M
) {
    companion object
}

/**
 * Extracts instance [T] from the specified [Binding].
 * ```kotlin
 * val someService = app.get { someService }
 * ```
 */
fun <M : Module, T : Any> Managed<M>.get(binding: M.() -> Binding<T>): T {
    contract {
        callsInPlace(binding, InvocationKind.EXACTLY_ONCE)
    }
    return module.binding().provider.get()
}

/**
 * Extracts instance [T] as a delegated property from the specified [Binding].
 * ```kotlin
 * val someService by app { someService }
 * ```
 */
operator fun <M : Module, T : Any> Managed<M>.invoke(binding: M.() -> Binding<T>) =
    object : ReadOnlyProperty<Nothing?, T> {
        override fun getValue(thisRef: Nothing?, property: KProperty<*>): T {
            return module.binding().provider.get()
        }
    }

/**
 * Refreshes this managed [Module]. All [Value]s and [Bean]s will be destroyed and reinitialized.
 *
 * It is possible to specify whether a certain bean is refreshable, see the [bean] declaration method.
 */
fun <M : Module> Managed<M>.refresh() {
    container.refresh()
}

/**
 * Destroys this instance meaning that all [Bean]s will be destroyed.
 */
fun <M : Module> Managed<M>.destroy() {
    container.destroy()
}
