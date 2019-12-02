/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal sealed class BindingState<out T : Any>

internal class DefiningBinding<T : Any>(
    internal val lazy: Boolean,
    supply: () -> T
) : BindingState<T>() {
    var onSupply: () -> T = supply
    var onStart: (T) -> Unit = {}
    var onStop: (T) -> Unit = {}
}

internal class InitializedBinding<T : Any>(val provider: Provider<T>, val lazy: Boolean) :
    BindingState<T>()

internal object DestroyedBinding : BindingState<Nothing>()

internal inline fun <reified T : BindingState<*>> BindingState<*>.runAs(block: T.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    if (this is T) {
        this.block()
    } else {
        throw IllegalStateException("Expected state to be ${T::class} but was ${this::class}")
    }
}

internal inline fun <reified T : BindingState<*>> BindingState<*>.runIf(block: T.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (this is T) {
        this.block()
    }
}
