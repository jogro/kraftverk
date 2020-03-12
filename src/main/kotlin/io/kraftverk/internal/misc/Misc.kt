/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.misc

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal typealias Consumer<T> = (T) -> Unit

internal typealias InstanceFactory<T> = () -> T

internal inline fun <reified T : Any> Any.applyWhen(block: T.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (this is T) {
        this.block()
    }
}

internal inline fun <reified T : Any> Any.applyAs(block: T.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    this.narrow<T>().block()
}

internal inline fun <reified T : Any> Any.narrow(): T {
    check(this is T) { "Expected this to be ${T::class} but was ${this::class}" }
    return this
}

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
