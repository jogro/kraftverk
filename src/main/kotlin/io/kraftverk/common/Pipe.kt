package io.kraftverk.common

import io.kraftverk.declaration.LifecycleActions
import io.kraftverk.internal.misc.interceptAfter

sealed class Pipe<in T : Any>

internal val <T : Any> Pipe<T>.delegate
    get() = when (this) {
        is PipeImpl -> delegate
    }

internal class PipeImpl<T : Any>(val delegate: PipeDelegate<T>) : Pipe<T>()

internal class PipeDelegate<T : Any> {

    var onConfigure: (T, LifecycleActions) -> Unit = { _, _ -> }

    fun configure(block: (T, LifecycleActions) -> Unit) {
        onConfigure = interceptAfter(onConfigure, block)
    }
}
