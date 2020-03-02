package io.kraftverk.managed.operations

import io.kraftverk.binding.Binding
import io.kraftverk.binding.provider
import io.kraftverk.managed.Managed
import io.kraftverk.module.Module
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Extraction of instance [T] from the specified [Binding] in [Module] M.
 * ```kotlin
 * val someService = app { someService }
 * ```
 */
operator fun <M : Module, T : Any> Managed<M>.invoke(binding: M.() -> Binding<T>): T {
    return module.binding().provider.get()
}

/**
 * Lazy extraction of instance [T] from the specified [Binding] in [Module] M.
 * ```kotlin
 * val someService by app.get { someService }
 * ```
 */
fun <M : Module, T : Any> Managed<M>.get(binding: M.() -> Binding<T>) =
    object : ReadOnlyProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return invoke(binding)
        }
    }
