/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.container

import io.kraftverk.binding.BeanImpl
import io.kraftverk.binding.Binding
import io.kraftverk.binding.Component
import io.kraftverk.binding.ComponentImpl
import io.kraftverk.binding.Value
import io.kraftverk.binding.ValueImpl
import io.kraftverk.binding.handler
import io.kraftverk.common.ComponentDefinition
import io.kraftverk.common.ValueDefinition
import io.kraftverk.internal.binding.ComponentHandler
import io.kraftverk.internal.binding.ValueHandler
import io.kraftverk.internal.binding.initialize
import io.kraftverk.internal.binding.start
import io.kraftverk.internal.container.Container.State
import io.kraftverk.internal.misc.mustBe

internal fun <T : Any, S : Any> Container.createComponent(
    config: ComponentDefinition<T, S>
): ComponentImpl<T, S> = config.let(::process)
    .let(::ComponentHandler)
    .let(::ComponentImpl)
    .also(this::register)

internal fun <T : Any> Container.createBean(
    config: ComponentDefinition<T, T>
): BeanImpl<T> = config.let(::process)
    .let(::ComponentHandler)
    .let(::BeanImpl)
    .also(this::register)

internal fun <T : Any> Container.createValue(
    config: ValueDefinition<T>
): ValueImpl<T> = config.let(::process)
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

private fun <T : Any, S : Any> Container.process(config: ComponentDefinition<T, S>): ComponentDefinition<T, S> {
    var current = config
    state.mustBe<State.Configurable> {
        for (processor in componentProcessors) {
            current = processor.process(current)
        }
    }
    return current
}

private fun <T : Any> Container.process(config: ValueDefinition<T>): ValueDefinition<T> {
    var current = config
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
    filterIsInstance<Component<*, *>>().forEach { component -> component.handler.initialize(lazy) }
}
