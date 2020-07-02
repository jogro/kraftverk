package io.kraftverk.core.internal.container

import io.kraftverk.core.binding.ValueImpl
import io.kraftverk.core.common.ValueDefinition
import io.kraftverk.core.common.ValueProcessor
import io.kraftverk.core.internal.binding.ValueDelegate
import io.kraftverk.core.internal.binding.ValueProviderFactory

internal class ValueFactory(private val valueProcessors: List<ValueProcessor>) {

    fun <T : Any> createValue(definition: ValueDefinition<T>): ValueImpl<T> =
        definition.let(::process)
            .let(::ValueProviderFactory)
            .let(::ValueDelegate)
            .let(::ValueImpl)

    private fun <T : Any> process(definition: ValueDefinition<T>): ValueDefinition<T> {
        var current = definition
        for (processor in valueProcessors) {
            current = processor.process(current)
        }
        return current
    }
}
