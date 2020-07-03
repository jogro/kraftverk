package io.kraftverk.core.module

import io.kraftverk.core.common.Pipe
import io.kraftverk.core.common.PipeDelegate
import io.kraftverk.core.common.PipeImpl
import io.kraftverk.core.declaration.PipeDeclaration
import kotlin.properties.ReadOnlyProperty

fun <T : Any> BasicModule<*>.pipe(block: PipeDeclaration<T>.(T) -> Unit = { }):
        ReadOnlyProperty<BasicModule<*>, Pipe<T>> {
    val pipe: PipeImpl<T> =
        PipeImpl(PipeDelegate())
    configure(pipe, block)
    return Delegate(pipe)
}
