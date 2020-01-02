package io.kraftverk.internal

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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
    if (this is T) {
        this.block()
    } else {
        throw IllegalStateException("Expected this to be ${T::class} but was ${this::class}")
    }
}

private val spinalRegex = "([A-Z]+)".toRegex()

internal fun String.spinalCase() = replace(spinalRegex, "\\-$1").toLowerCase()