/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.BeanImpl
import io.kraftverk.Component
import io.kraftverk.PropertyImpl

internal fun <T : Any> Component<T>.onSupply(
    block: (() -> T) -> T
) {
    this.toBinding().onSupply(block)
}

internal fun <T : Any> Component<T>.onStart(
    block: (T, (T) -> Unit) -> Unit
) {
    this.toBinding().onStart(block)
}

internal fun <T : Any> Component<T>.onStop(
    block: (T, (T) -> Unit) -> Unit
) {
    this.toBinding().onStop(block)
}

internal fun Component<*>.initialize() {
    this.toBinding().initialize()
}

internal fun Component<*>.start() {
    this.toBinding().start()
}

internal fun Component<*>.destroy() {
    this.toBinding().destroy()
}

internal fun <T : Any> Component<T>.provider(): Provider<T> {
    return this.toBinding().provider()
}

private fun <T : Any> Component<T>.toBinding(): Binding<T> = when (this) {
    is BeanImpl<T> -> binding
    is PropertyImpl<T> -> binding
}
