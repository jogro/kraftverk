/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.provider

import io.kraftverk.internal.provider.Singleton

sealed class Provider<out T : Any>(
    val name: String,
    val lazy: Boolean
)

sealed class BeanProvider<T : Any>(
    name: String,
    lazy: Boolean
) : Provider<T>(name, lazy)

sealed class ValueProvider<T : Any>(
    name: String,
    lazy: Boolean
) : Provider<T>(name, lazy)

fun <T : Any> Provider<T>.get(): T = singleton.get()
val <T : Any> Provider<T>.type get() = singleton.type
val <T : Any> Provider<T>.instanceId get() = singleton.instanceId

internal class BeanProviderImpl<T : Any> constructor(
    name: String,
    lazy: Boolean,
    val singleton: Singleton<T>
) : BeanProvider<T>(name, lazy)

internal class ValueProviderImpl<T : Any> constructor(
    name: String,
    lazy: Boolean,
    val singleton: Singleton<T>
) : ValueProvider<T>(name, lazy)

internal fun <T : Any> Provider<T>.destroy() = singleton.destroy()
internal fun <T : Any> Provider<T>.initialize() = singleton.initialize()

internal val <T : Any> Provider<T>.singleton
    get() = when (this) {
        is BeanProviderImpl<T> -> singleton
        is ValueProviderImpl<T> -> singleton
    }
