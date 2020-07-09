/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.core.managed

import io.kraftverk.core.binding.Binding
import io.kraftverk.core.binding.delegate
import io.kraftverk.core.common.BeanProcessor
import io.kraftverk.core.common.ValueProcessor
import io.kraftverk.core.internal.container.beanProviders
import io.kraftverk.core.internal.container.start
import io.kraftverk.core.internal.container.stop
import io.kraftverk.core.internal.container.valueProviders
import io.kraftverk.core.internal.logging.createLogger
import io.kraftverk.core.internal.misc.BasicState
import io.kraftverk.core.internal.misc.mightBe
import io.kraftverk.core.internal.misc.mustBe
import io.kraftverk.core.module.Module
import io.kraftverk.core.provider.BeanProvider
import io.kraftverk.core.provider.ValueProvider
import io.kraftverk.core.provider.get
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
class Managed<M : Module> internal constructor(moduleFactory: ModuleFactory<M>) {

    internal val logger = createLogger { }

    @Volatile
    internal var state: State<M> =
        State.Configurable(moduleFactory)

    internal sealed class State<out M : Module> :
        BasicState {

        class Configurable<M : Module>(
            val moduleFactory: ModuleFactory<M>
        ) : State<M>()

        class Running<M : Module>(
            val module: M
        ) : State<M>()

        object Destroying : State<Nothing>()

        object Destroyed : State<Nothing>()
    }

    /**
     * The [start] function is by default non-lazy, meaning that:
     * 1) All value bindings declared in the [Module] are eagerly looked up using the Environment that
     * was specified at the time the managed instance was created. Should any value be missing an exception is thrown.
     * 2) All Bean bindings are eagerly instantiated.
     *
     * Call the [Managed.stop] method to destroy the [Managed] instance.
     */
    fun start(lazy: Boolean = false, block: M.() -> Unit = {}): Managed<M> {
        logger.info { "Starting managed module" }
        val startMs = System.currentTimeMillis()
        state.mustBe<State.Configurable<M>> {
            moduleFactory.configure(block)
            val module = moduleFactory.createModule(lazy)
            state = State.Running(module)
        }
        Runtime.getRuntime().addShutdownHook(Thread { stop() })
        logger.info { "Started managed module in ${System.currentTimeMillis() - startMs}ms" }
        return this
    }

    fun configure(block: M.() -> Unit): Managed<M> {
        state.mustBe<State.Configurable<M>> {
            moduleFactory.configure(block)
        }
        return this
    }

    fun addProcessor(processor: BeanProcessor): Managed<M> {
        state.mustBe<State.Configurable<M>> {
            moduleFactory.addProcessor(processor)
        }
        return this
    }

    fun addProcessor(processor: ValueProcessor): Managed<M> {
        state.mustBe<State.Configurable<M>> {
            moduleFactory.addProcessor(processor)
        }
        return this
    }

    /**
     * Retrieves all [BeanProvider]s.
     */
    val beanProviders: List<BeanProvider<*>> get() = module.container.beanProviders

    /**
     * Retrieves all [ValueProvider]s.
     */
    val valueProviders: List<ValueProvider<*>> get() = module.container.valueProviders

    /**
     * Extraction of instance [T] from the specified [Binding] in [Module] M.
     * ```kotlin
     * val someService = app { someService }
     * ```
     */
    operator fun <T : Any> invoke(binding: M.() -> Binding<T>): T {
        state.mustBe<State.Running<M>> {
            return module.binding().delegate.provider.get()
        }
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

    /**
     * Stops this instance.
     */
    fun stop() {
        state.mightBe<State.Running<*>> {
            logger.info { "Stopping managed module" }
            state = State.Destroying
            module.container.stop()
            state = State.Destroyed
            logger.info { "Stopped managed module" }
        }
    }

    internal val module: M
        get() {
            state.mustBe<State.Running<M>> {
                return module
            }
        }

    internal companion object
}
