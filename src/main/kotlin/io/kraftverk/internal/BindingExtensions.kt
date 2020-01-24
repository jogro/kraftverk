/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.BeanImpl
import io.kraftverk.Binding
import io.kraftverk.ValueImpl

internal fun <T : Any> Binding<T>.onBind(
    block: (() -> T) -> T
) {
    this.toHandler().onBind(block)
}

internal fun <T : Any> Binding<T>.onCreate(
    block: (T, (T) -> Unit) -> Unit
) {
    this.toHandler().onCreate(block)
}

internal fun <T : Any> Binding<T>.onDestroy(
    block: (T, (T) -> Unit) -> Unit
) {
    this.toHandler().onDestroy(block)
}

internal fun Binding<*>.start() {
    this.toHandler().start()
}

internal fun Binding<*>.refresh() {
    this.toHandler().refresh()
}

internal fun Binding<*>.prepare() {
    this.toHandler().prepare()
}

internal fun Binding<*>.destroy() {
    this.toHandler().destroy()
}

internal fun <T : Any> Binding<T>.provider(): Provider<T> {
    return this.toHandler().provider()
}

internal val Binding<*>.container: Container get() = toHandler().container

private fun <T : Any> Binding<T>.toHandler(): BindingHandler<T> = when (this) {
    is BeanImpl<T> -> handler
    is ValueImpl<T> -> handler
}
