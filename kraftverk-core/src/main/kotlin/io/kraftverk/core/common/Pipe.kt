package io.kraftverk.core.common

import io.kraftverk.core.internal.misc.interceptAfter

sealed class Pipe<in T : Any>

internal val <T : Any> Pipe<T>.delegate
    get() = when (this) {
        is PipeImpl -> delegate
    }

internal class PipeImpl<T : Any>(val delegate: PipeDelegate<T>) : Pipe<T>()

internal class PipeDelegate<T : Any> {

    var onPipe: (T) -> Unit = { }

    fun configure(block: (T) -> Unit) {
        onPipe = interceptAfter(onPipe, block)
    }
}
