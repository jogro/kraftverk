package io.kraftverk.internal.container

import io.kraftverk.binding.ValueImpl
import io.kraftverk.common.ValueDefinition
import io.kraftverk.common.ValueProcessor
import io.kraftverk.internal.binding.ValueHandler
import io.kraftverk.internal.binding.ValueProviderFactory

internal class ValueFactory(private val valueProcessors: List<ValueProcessor>) {

    fun <T : Any> createValue(definition: ValueDefinition<T>): ValueImpl<T> =
        definition.let(::process)
            .let(::ValueProviderFactory)
            .let(::ValueHandler)
            .let(::ValueImpl)

    private fun <T : Any> process(definition: ValueDefinition<T>): ValueDefinition<T> {
        var current = definition
        for (processor in valueProcessors) {
            current = processor.process(current)
        }
        return current
    }
}
