/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.misc

internal typealias Consumer<T> = (T) -> Unit

internal typealias Supplier<T> = () -> T

internal inline fun <T : Any> interceptAround(
    noinline supplier: Supplier<T>,
    crossinline block: (Supplier<T>) -> T
): Supplier<T> = {
    block(supplier)
}

internal inline fun <T : Any> interceptAround(
    noinline consumer: Consumer<T>,
    crossinline block: (T, Consumer<T>) -> Unit
): Consumer<T> = { t ->
    block(t, consumer)
}

internal inline fun <T : Any> interceptAfter(
    noinline consumer: Consumer<T>,
    crossinline block: Consumer<T>
): Consumer<T> = { t ->
    consumer(t)
    block(t)
}
