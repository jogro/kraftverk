/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.provider

import io.kraftverk.internal.provider.Singleton

sealed class Provider<out T : Any>

sealed class BeanProvider<T : Any> : Provider<T>()

sealed class ValueProvider<T : Any> : Provider<T>()

fun <T : Any> Provider<T>.get(): T = singleton.get()
val <T : Any> Provider<T>.type get() = singleton.type
val <T : Any> Provider<T>.lazy get() = singleton.lazy
val <T : Any> Provider<T>.instanceId get() = singleton.instanceId

internal class BeanProviderImpl<T : Any> constructor(
    val singleton: Singleton<T>
) : BeanProvider<T>()

internal class ValueProviderImpl<T : Any> constructor(
    val singleton: Singleton<T>
) : ValueProvider<T>()

internal fun <T : Any> Provider<T>.destroy() = singleton.destroy()
internal fun <T : Any> Provider<T>.initialize() = singleton.initialize()

internal val <T : Any> Provider<T>.singleton
    get() = when (this) {
        is BeanProviderImpl<T> -> singleton
        is ValueProviderImpl<T> -> singleton
    }
