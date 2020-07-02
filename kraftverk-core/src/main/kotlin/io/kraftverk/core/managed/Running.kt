/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.core.managed

import io.kraftverk.core.binding.Binding
import io.kraftverk.core.binding.delegate
import io.kraftverk.core.internal.container.beanProviders
import io.kraftverk.core.internal.container.stop
import io.kraftverk.core.internal.container.valueProviders
import io.kraftverk.core.internal.misc.mightBe
import io.kraftverk.core.internal.misc.mustBe
import io.kraftverk.core.managed.Managed.State
import io.kraftverk.core.module.Module
import io.kraftverk.core.provider.BeanProvider
import io.kraftverk.core.provider.ValueProvider
import io.kraftverk.core.provider.get
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Retrieves all [BeanProvider]s.
 */
val Managed<*>.beanProviders: List<BeanProvider<*>> get() = module.container.beanProviders

/**
 * Retrieves all [ValueProvider]s.
 */
val Managed<*>.valueProviders: List<ValueProvider<*>> get() = module.container.valueProviders

/**
 * Extraction of instance [T] from the specified [Binding] in [Module] M.
 * ```kotlin
 * val someService = app { someService }
 * ```
 */
operator fun <T : Any, M : Module> Managed<M>.invoke(binding: M.() -> Binding<T>): T {
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
fun <T : Any, M : Module> Managed<M>.get(binding: M.() -> Binding<T>) =
    object : ReadOnlyProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return invoke(binding)
        }
    }

/**
 * Stops this instance.
 */
fun <M : Module> Managed<M>.stop() {
    state.mightBe<State.Running<*>> {
        logger.info { "Stopping managed module" }
        state = State.Destroying
        module.container.stop()
        state = State.Destroyed
        logger.info { "Stopped managed module" }
    }
}

internal val <M : Module> Managed<M>.module: Module
    get() {
        state.mustBe<State.Running<M>> {
            return module
        }
    }
