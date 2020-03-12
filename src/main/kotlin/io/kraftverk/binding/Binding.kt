/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.binding

import io.kraftverk.internal.binding.BeanHandler
import io.kraftverk.internal.binding.BindingHandler
import io.kraftverk.internal.binding.ValueHandler
import io.kraftverk.internal.binding.provider
import io.kraftverk.provider.Provider

sealed class Binding<out T : Any>

sealed class Bean<out T : Any> : Binding<T>() {
    companion object
}

sealed class Value<out T : Any> : Binding<T>() {
    companion object
}

internal class BeanImpl<T : Any>(val handler: BeanHandler<T>) : Bean<T>()
internal class ValueImpl<T : Any>(val handler: ValueHandler<T>) : Value<T>()

internal val <T : Any> Bean<T>.handler: BeanHandler<T>
    get() = when (this) {
        is BeanImpl<T> -> handler
    }

internal val <T : Any> Value<T>.handler: ValueHandler<T>
    get() = when (this) {
        is ValueImpl<T> -> handler
    }

internal val <T : Any> Binding<T>.handler: BindingHandler<T, Provider<T>>
    get() = when (this) {
        is BeanImpl<T> -> handler
        is ValueImpl<T> -> handler
    }

internal val <T : Any> Binding<T>.provider: Provider<T> get() = handler.provider
