package io.kraftverk.core.common

import io.kraftverk.core.declaration.LifecycleActions
import io.kraftverk.core.internal.misc.interceptAfter

sealed class Pipe<in T : Any>

internal val <T : Any> Pipe<T>.delegate
    get() = when (this) {
        is PipeImpl -> delegate
    }

internal class PipeImpl<T : Any>(val delegate: PipeDelegate<T>) : Pipe<T>()

internal class PipeDelegate<T : Any> {

    var onPipe: (T, LifecycleActions) -> Unit = { _, _ -> }

    fun configure(block: (T, LifecycleActions) -> Unit) {
        onPipe = interceptAfter(onPipe, block)
    }
}
