/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.managed

import io.kraftverk.binding.Binding
import io.kraftverk.binding.provider
import io.kraftverk.internal.managed.BasicManaged
import io.kraftverk.internal.misc.applyAs
import io.kraftverk.internal.misc.applyWhen
import io.kraftverk.module.Module
import io.kraftverk.provider.BeanProvider
import io.kraftverk.provider.ValueProvider
import io.kraftverk.provider.get
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Provides access to and manages bean and value instances in a specified
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
 * Call the manage factory function to create a [Managed] instance of the module.
 * ```kotlin
 * val app by Kraftverk.manage { AppModule() }
 * ```
 */
class Managed<M : Module> internal constructor(
    module: M
) : BasicManaged<M>(module) {

    /**
     * Retrieves all [BeanProvider]s.
     */
    val beanProviders: List<BeanProvider<*>> by lazy(module::beanProviders)

    /**
     * Retrieves all [ValueProvider]s.
     */
    val valueProviders: List<ValueProvider<*>> by lazy(module::valueProviders)

    /**
     * The [start] function will by default perform the following actions:
     * 1) All value bindings declared in the [Module] are eagerly looked up using the Environment that
     * was specified at the time the managed instance was created.
     * Should any value be missing an exception is thrown.
     * 2) All Bean bindings are eagerly instantiated.
     *
     * Call the [Managed.stop] method to destroy the [Managed] instance.
     */
    fun start(block: M.() -> Unit = {}): Managed<M> {
        logger.info { "Starting module" }
        val startMs = System.currentTimeMillis()
        customize(block)
        state.applyAs<State.Defining<M>> {
            onStart(module)
            module.start()
            state = State.Started(module)
        }
        Runtime.getRuntime().addShutdownHook(Thread {
            stop()
        })
        logger.info { "Started module in ${System.currentTimeMillis() - startMs}ms" }
        return this
    }

    /**
     * Extraction of instance [T] from the specified [Binding] in [Module] M.
     * ```kotlin
     * val someService = app { someService }
     * ```
     */
    operator fun <T : Any> invoke(binding: M.() -> Binding<T>): T {
        return module.binding().provider.get()
    }

    /**
     * Lazy extraction of instance [T] from the specified [Binding] in [Module] M.
     * ```kotlin
     * val someService by app.get { someService }
     * ```
     */
    fun <T : Any> get(binding: M.() -> Binding<T>) =
        object : ReadOnlyProperty<Any?, T> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): T {
                return invoke(binding)
            }
        }

    fun customize(block: M.() -> Unit): Managed<M> {
        state.applyAs<State.Defining<M>> {
            val previousOnStart = onStart
            onStart = { instance ->
                previousOnStart(instance)
                block(instance)
            }
        }
        return this
    }

    /**
     * Stops this instance meaning that all beans will be destroyed.
     */
    fun stop() {
        state.applyWhen<State.Started<*>> {
            state = State.Destroying
            module.stop()
            state = State.Destroyed
        }
    }
}
