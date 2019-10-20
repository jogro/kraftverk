/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

internal sealed class BindingState<out T : Any>

internal class DefiningBinding<T : Any>(
    internal val lazy: Boolean,
    supply: () -> T
) : BindingState<T>() {
    var onSupply: () -> T = supply
    var onStart: (T) -> Unit = {}
    var onStop: (T) -> Unit = {}
}

internal class InitializedBinding<T : Any, P : Provider<T>>(val provider: P, val lazy: Boolean) :
    BindingState<T>()

internal object DestroyedBinding : BindingState<Nothing>()
