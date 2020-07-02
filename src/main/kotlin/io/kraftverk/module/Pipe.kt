package io.kraftverk.module

import io.kraftverk.common.Pipe
import io.kraftverk.common.PipeDelegate
import io.kraftverk.common.PipeImpl
import io.kraftverk.declaration.PipeDeclaration
import kotlin.properties.ReadOnlyProperty

fun <T : Any> BasicModule<*>.sink(block: PipeDeclaration<T>.(T) -> Unit = { }):
        ReadOnlyProperty<BasicModule<*>, Pipe<T>> {
    val sink: PipeImpl<T> = PipeImpl(PipeDelegate())
    configure(sink, block)
    return Delegate(sink)
}
