package io.kraftverk.internal.misc

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal interface BasicState

internal inline fun <reified T : Any> BasicState.mightBe(block: T.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (this is T) {
        this.block()
    }
}

internal inline fun <reified T : Any> BasicState.mustBe(block: T.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    this.narrow<T>().block()
}

internal inline fun <reified T : Any> BasicState.narrow(): T {
    check(this is T) {
        "Expected state to be '${T::class.simpleName}' but was '${this::class.simpleName}'"
    }
    return this
}
