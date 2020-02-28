/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal typealias Consumer<T> = (T) -> Unit

internal typealias InstanceSupplier<T> = () -> T

internal typealias ProviderFactory<T> = (
    createInstance: InstanceSupplier<T>,
    onCreate: Consumer<T>,
    onDestroy: Consumer<T>
) -> Provider<T>

internal class ThreadBound<T> {

    private val threadLocal = ThreadLocal<T>()

    fun get(): T = threadLocal.get() ?: throw IllegalStateException()

    fun <R> use(value: T, block: () -> R): R {
        val previous: T? = threadLocal.get()
        threadLocal.set(value)
        try {
            return block()
        } finally {
            if (previous == null) {
                threadLocal.remove()
            } else {
                threadLocal.set(previous)
            }
        }
    }
}

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

private val spinalRegex = "([A-Z]+)".toRegex()

internal fun String.toSpinalCase() = replace(spinalRegex, "\\-$1").toLowerCase()
