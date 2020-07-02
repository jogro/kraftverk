package io.kraftverk.core.module

import io.kraftverk.core.common.Pipe
import io.kraftverk.core.common.PipeDelegate
import io.kraftverk.core.common.PipeImpl
import io.kraftverk.core.declaration.PipeDeclaration
import kotlin.properties.ReadOnlyProperty

fun <T : Any> BasicModule<*>.sink(block: PipeDeclaration<T>.(T) -> Unit = { }):
        ReadOnlyProperty<BasicModule<*>, Pipe<T>> {
    val sink: PipeImpl<T> =
        PipeImpl(PipeDelegate())
    configure(sink, block)
    return Delegate(sink)
}
