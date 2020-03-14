/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.provider

import io.kraftverk.common.BeanConfig
import io.kraftverk.common.BindingConfig
import io.kraftverk.common.ValueConfig
import io.kraftverk.internal.provider.Singleton

sealed class Provider<out T : Any>

sealed class BeanProvider<T : Any> : Provider<T>()

sealed class ValueProvider<T : Any> : Provider<T>()

fun <T : Any> Provider<T>.get(): T = singleton.get()
val <T : Any> Provider<T>.type get() = singleton.type
val <T : Any> Provider<T>.instanceId get() = singleton.instanceId

val <T : Any> Provider<T>.config: BindingConfig<T>
    get() = when (this) {
        is BeanProviderImpl<T> -> config
        is ValueProviderImpl<T> -> config
    }

val <T : Any> BeanProvider<T>.config: BeanConfig<T>
    get() = when (this) {
        is BeanProviderImpl<T> -> config
    }

val <T : Any> ValueProvider<T>.config: ValueConfig<T>
    get() = when (this) {
        is ValueProviderImpl<T> -> config
    }

internal class BeanProviderImpl<T : Any> constructor(
    val config: BeanConfig<T>,
    val singleton: Singleton<T>
) : BeanProvider<T>()

internal class ValueProviderImpl<T : Any> constructor(
    val config: ValueConfig<T>,
    val singleton: Singleton<T>
) : ValueProvider<T>()

internal fun <T : Any> Provider<T>.destroy() = singleton.destroy()
internal fun <T : Any> Provider<T>.initialize() = singleton.initialize()

internal val <T : Any> Provider<T>.singleton
    get() = when (this) {
        is BeanProviderImpl<T> -> singleton
        is ValueProviderImpl<T> -> singleton
    }
