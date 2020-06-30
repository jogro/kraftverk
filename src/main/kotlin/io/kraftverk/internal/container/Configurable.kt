/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.container

import io.kraftverk.binding.Bean
import io.kraftverk.binding.BeanImpl
import io.kraftverk.binding.Binding
import io.kraftverk.binding.Value
import io.kraftverk.binding.ValueImpl
import io.kraftverk.binding.handler
import io.kraftverk.common.BeanDefinition
import io.kraftverk.common.ValueDefinition
import io.kraftverk.internal.binding.BeanHandler
import io.kraftverk.internal.binding.BeanProviderFactory
import io.kraftverk.internal.binding.ValueHandler
import io.kraftverk.internal.binding.ValueProviderFactory
import io.kraftverk.internal.container.Container.State
import io.kraftverk.internal.misc.mustBe

internal fun <T : Any> Container.createBean(
    definition: BeanDefinition<T, T>
): BeanImpl<T> = definition.let(::process)
    .let(::BeanProviderFactory)
    .let(::BeanHandler)
    .let(::BeanImpl)
    .also(this::register)

internal fun <T : Any> Container.createValue(
    definition: ValueDefinition<T>
): ValueImpl<T> = definition.let(::process)
    .let(::ValueProviderFactory)
    .let(::ValueHandler)
    .let(::ValueImpl)
    .also(this::register)

private fun Container.register(binding: Binding<*>) =
    state.mustBe<State.Configurable> {
        bindings.add(binding)
    }

internal fun Container.start(lazy: Boolean) =
    state.mustBe<State.Configurable> {
        bindings.start()
        state = State.Running(bindings.toList())
        bindings.initialize(lazy)
    }

private fun <T : Any, S : Any> Container.process(definition: BeanDefinition<T, S>): BeanDefinition<T, S> {
    var current = definition
    state.mustBe<State.Configurable> {
        for (processor in beanProcessors) {
            current = processor.process(current)
        }
    }
    return current
}

private fun <T : Any> Container.process(definition: ValueDefinition<T>): ValueDefinition<T> {
    var current = definition
    state.mustBe<State.Configurable> {
        for (processor in valueProcessors) {
            current = processor.process(current)
        }
    }
    return current
}

private fun List<Binding<*>>.start() {
    forEach { binding -> binding.handler.start() }
}

private fun List<Binding<*>>.initialize(lazy: Boolean) {
    val valueNotFoundExceptions = mutableListOf<ValueNotFoundException>()
    filterIsInstance<Value<*>>().forEach { value ->
        try {
            value.handler.initialize(lazy)
        } catch (e: ValueNotFoundException) {
            valueNotFoundExceptions += e
        }
    }
    valueNotFoundExceptions.takeIf { it.isNotEmpty() }
        ?.joinToString(separator = "\n") { "      - " + it.valueName }
        ?.let { errorMsg ->
            val exceptionMessage = """


Couldn't initialize the container since the following values seem to be missing:
$errorMsg

                """.trimIndent()
            throw IllegalStateException(exceptionMessage)
        }
    filterIsInstance<Bean<*>>().forEach { bean -> bean.handler.initialize(lazy) }
}
