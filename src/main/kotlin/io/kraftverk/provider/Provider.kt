/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.provider

import io.kraftverk.common.BeanDefinition
import io.kraftverk.common.BindingDefinition
import io.kraftverk.common.ValueDefinition
import io.kraftverk.internal.provider.Singleton

sealed class Provider<out T : Any>

sealed class BeanProvider<T : Any> : Provider<T>()

sealed class ValueProvider<T : Any> : Provider<T>()

fun <T : Any> Provider<T>.get(): T = singleton.get()
val <T : Any> Provider<T>.type get() = singleton.type
val <T : Any> Provider<T>.instanceId get() = singleton.instanceId

val <T : Any> Provider<T>.definition: BindingDefinition<T>
    get() = when (this) {
        is BeanProviderImpl<T> -> definition
        is ValueProviderImpl<T> -> definition
    }

val <T : Any> BeanProvider<T>.definition: BeanDefinition<T>
    get() = when (this) {
        is BeanProviderImpl<T> -> definition
    }

val <T : Any> ValueProvider<T>.definition: ValueDefinition<T>
    get() = when (this) {
        is ValueProviderImpl<T> -> definition
    }

internal class BeanProviderImpl<T : Any> constructor(
    val definition: BeanDefinition<T>,
    val singleton: Singleton<T>
) : BeanProvider<T>()

internal class ValueProviderImpl<T : Any> constructor(
    val definition: ValueDefinition<T>,
    val singleton: Singleton<T>
) : ValueProvider<T>()

internal fun <T : Any> Provider<T>.destroy() = singleton.destroy()
internal fun <T : Any> Provider<T>.initialize(lazy: Boolean) {
    singleton.initialize(lazy)
}

internal val <T : Any> Provider<T>.singleton
    get() = when (this) {
        is BeanProviderImpl<T> -> singleton
        is ValueProviderImpl<T> -> singleton
    }
