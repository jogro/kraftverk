/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.managed

import io.kraftverk.binding.Binding
import io.kraftverk.binding.provider
import io.kraftverk.internal.container.beanProviders
import io.kraftverk.internal.container.stop
import io.kraftverk.internal.container.valueProviders
import io.kraftverk.internal.misc.mightBe
import io.kraftverk.internal.misc.mustBe
import io.kraftverk.module.Module
import io.kraftverk.provider.BeanProvider
import io.kraftverk.provider.ValueProvider
import io.kraftverk.provider.get
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
    state.mustBe<Managed.State.Running<M>> {
        return module.binding().provider.get()
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
 * Stops this instance meaning that all beans will be destroyed.
 */
fun <M : Module> Managed<M>.stop() {
    state.mightBe<Managed.State.Running<*>> {
        state = Managed.State.Destroying
        module.container.stop()
        state = Managed.State.Destroyed
    }
}

internal val <M : Module> Managed<M>.module: Module
    get() {
        state.mustBe<Managed.State.Running<M>> {
            return module
        }
    }
