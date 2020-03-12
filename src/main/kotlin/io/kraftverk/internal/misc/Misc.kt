/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.misc

internal typealias Consumer<T> = (T) -> Unit

internal typealias InstanceFactory<T> = () -> T

internal inline fun <T : Any> intercept(
    noinline supplier: () -> T,
    crossinline block: (() -> T) -> T
): () -> T = {
    block(supplier)
}

internal inline fun <T : Any> intercept(
    noinline consumer: (T) -> Unit,
    crossinline block: (T, (T) -> Unit) -> Unit
): (T) -> Unit = { t ->
    block(t, consumer)
}
