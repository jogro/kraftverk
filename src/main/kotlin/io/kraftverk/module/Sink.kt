package io.kraftverk.module

import io.kraftverk.common.Sink
import io.kraftverk.common.SinkDelegate
import io.kraftverk.common.SinkImpl
import io.kraftverk.declaration.SinkDeclaration
import kotlin.properties.ReadOnlyProperty

fun <T : Any> BasicModule<*>.sink(block: SinkDeclaration<T>.(T) -> Unit = { }):
        ReadOnlyProperty<BasicModule<*>, Sink<T>> {
    val sink: SinkImpl<T> = SinkImpl(SinkDelegate())
    configure(sink, block)
    return Delegate(sink)
}
