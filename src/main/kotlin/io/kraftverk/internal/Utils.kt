package io.kraftverk.internal

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun <reified T : Any> Any.applyWhen(block: T.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (this is T) {
        this.block()
    }
}

inline fun <reified T : Any> Any.applyAs(block: T.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    if (this is T) {
        this.block()
    } else {
        throw IllegalStateException("Expected this to be ${T::class} but was ${this::class}")
    }
}