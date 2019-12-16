/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.BeanImpl
import io.kraftverk.Binding
import io.kraftverk.PropertyImpl

internal fun <T : Any> Binding<T>.onBind(
    block: (() -> T) -> T
) {
    this.toDelegate().onBind(block)
}

internal fun <T : Any> Binding<T>.onCreate(
    block: (T, (T) -> Unit) -> Unit
) {
    this.toDelegate().onCreate(block)
}

internal fun <T : Any> Binding<T>.onDestroy(
    block: (T, (T) -> Unit) -> Unit
) {
    this.toDelegate().onDestroy(block)
}

internal fun Binding<*>.initialize() {
    this.toDelegate().initialize()
}

internal fun Binding<*>.evaluate() {
    this.toDelegate().evaluate()
}

internal fun Binding<*>.destroy() {
    this.toDelegate().destroy()
}

internal fun <T : Any> Binding<T>.provider(): Provider<T> {
    return this.toDelegate().provider()
}

private fun <T : Any> Binding<T>.toDelegate(): BindingDelegate<T> = when (this) {
    is BeanImpl<T> -> delegate
    is PropertyImpl<T> -> delegate
}
