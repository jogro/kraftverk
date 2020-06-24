/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.binding

import io.kraftverk.internal.binding.BindingHandler
import io.kraftverk.internal.binding.ComponentHandler
import io.kraftverk.internal.binding.ValueHandler

/**
 * A Bean is a [Component] that can be declared within a Kraftverk managed module.
 *
 * The primary purpose of a Bean is to serve as a configurable factory that produces injectable singleton
 * instances of type [T].
 *
 * See the [bean][io.kraftverk.module.bean] method on how to declare a Bean.
 */
sealed class Bean<out T : Any> : Component<T>() {
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
 * A CustomBean has the same characteristics as a Bean[Bean] but can be shaped by use of an intermediary
 * type S[S].
 *
 * See customBean[io.kraftverk.module.customBean] on how to declare a CustomBean.
 */
sealed class CustomBean<out T : Any, out S : Any> : Component<T>() {
    companion object
}

/**
 * Represents a [Bean] or [CustomBean]
 */
sealed class Component<out T : Any> : Binding<T>()

/**
 * A Binding is a [Component] or [Value].
 */
sealed class Binding<out T : Any>

/* Internals */

internal class BeanImpl<T : Any>(val handler: ComponentHandler<T, T>) : Bean<T>()

internal class ValueImpl<T : Any>(val handler: ValueHandler<T>) : Value<T>()

internal class CustomBeanImpl<T : Any, S : Any>(val handler: ComponentHandler<T, S>) : CustomBean<T, S>()

internal val <T : Any, S : Any> CustomBean<T, S>.handler: ComponentHandler<T, S>
    get() = when (this) {
        is CustomBeanImpl<T, S> -> handler
    }

internal val <T : Any> Bean<T>.handler: ComponentHandler<T, T>
    get() = when (this) {
        is BeanImpl<T> -> handler
    }

internal val <T : Any> Value<T>.handler: ValueHandler<T>
    get() = when (this) {
        is ValueImpl<T> -> handler
    }

internal val <T : Any> Component<T>.handler: ComponentHandler<T, *>
    get() = when (this) {
        is BeanImpl<T> -> handler
        is CustomBeanImpl<T, *> -> handler
    }

internal val <T : Any> Binding<T>.handler: BindingHandler<T>
    get() = when (this) {
        is ValueImpl<T> -> handler
        is BeanImpl<T> -> handler
        is CustomBeanImpl<T, *> -> handler
    }
