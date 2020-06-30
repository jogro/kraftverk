/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.binding

import io.kraftverk.internal.binding.BeanDelegate
import io.kraftverk.internal.binding.BindingDelegate
import io.kraftverk.internal.binding.ValueDelegate

/**
 * A Bean is a [Bean] that can be declared within a Kraftverk managed module.
 *
 * The primary purpose of a Bean is to serve as a configurable factory that produces injectable singleton
 * instances of type [T].
 *
 * See the [bean][io.kraftverk.module.bean] method on how to declare a Bean.
 */
sealed class Bean<out T : Any> : Binding<T>() {
    companion object
}

/**
 * A Value is a [Binding] that can be declared within a Kraftverk managed module.
 *
 * Values provide access to properties and environment variables that have been defined within the
 * [environment][io.kraftverk.env.Environment].
 *
 * The following methods can be used to declare a Value.
 *   - [value][io.kraftverk.module.value]
 *   - [string][io.kraftverk.module.string]
 *   - [int][io.kraftverk.module.int]
 *   - [long][io.kraftverk.module.long],
 *   - [boolean][io.kraftverk.module.boolean]
 *   - [port][io.kraftverk.module.port].
 */
sealed class Value<out T : Any> : Binding<T>() {
    companion object
}

/**
 * A Binding is a [Bean] or [Value].
 */
sealed class Binding<out T : Any>

/* Internals */

internal class BeanImpl<T : Any>(val delegate: BeanDelegate<T>) : Bean<T>()

internal class ValueImpl<T : Any>(val delegate: ValueDelegate<T>) : Value<T>()

internal val <T : Any> Bean<T>.delegate: BeanDelegate<T>
    get() = when (this) {
        is BeanImpl<T> -> delegate
    }

internal val <T : Any> Value<T>.delegate: ValueDelegate<T>
    get() = when (this) {
        is ValueImpl<T> -> delegate
    }

internal val <T : Any> Binding<T>.delegate: BindingDelegate<T>
    get() = when (this) {
        is ValueImpl<T> -> delegate
        is BeanImpl<T> -> delegate
    }
