/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.core.binding

import io.kraftverk.core.internal.binding.BeanDelegate
import io.kraftverk.core.internal.binding.BindingDelegate
import io.kraftverk.core.internal.binding.ValueDelegate

/**
 * A Bean is a [Binding] that can be declared within a Kraftverk managed module.
 *
 * The primary purpose of a Bean is to serve as a configurable factory that produces injectable singleton
 * instances of type [T].
 *
 * A Bean is basically a wrapper around a lambda expression of type () -> T. This lambda is guaranteed to
 * be run only ONCE, thus supplying a singleton instance. The lambda can only be called upon in the context of a
 * [BeanDeclaration][io.kraftverk.core.declaration.BeanDeclaration] by invoking the bean as a function
 * (operator invoke extension).
 *
 * See the [bean][io.kraftverk.core.module.Module.bean] method on how to declare a Bean.
 */
sealed class Bean<out T : Any> : Binding<T>() {
    companion object
}

/**
 * A Value is a [Binding] that can be declared within a Kraftverk managed module.
 *
 * Values provide access to properties and environment variables that have been defined within the
 * [environment][io.kraftverk.core.env.Environment].
 *
 * The following methods can be used to declare a Value.
 *   - [value][io.kraftverk.core.module.Module.value]
 *   - [string][io.kraftverk.core.module.string]
 *   - [int][io.kraftverk.core.module.int]
 *   - [long][io.kraftverk.core.module.long],
 *   - [boolean][io.kraftverk.core.module.boolean]
 *   - [port][io.kraftverk.core.module.port].
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
