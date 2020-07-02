package io.kraftverk.common

import io.kraftverk.declaration.LifecycleActions
import io.kraftverk.internal.misc.interceptAfter

sealed class Sink<in T : Any>

internal val <T : Any> Sink<T>.delegate
    get() = when (this) {
        is SinkImpl -> delegate
    }

internal class SinkImpl<T : Any>(val delegate: SinkDelegate<T>) : Sink<T>()

internal class SinkDelegate<T : Any> {

    var onConfigure: (T, LifecycleActions) -> Unit = { _, _ -> }

    fun configure(block: (T, LifecycleActions) -> Unit) {
        onConfigure = interceptAfter(onConfigure, block)
    }
}
