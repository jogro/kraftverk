/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.container

// import io.kraftverk.binding.handler
import io.kraftverk.binding.Bean
import io.kraftverk.binding.BeanImpl
import io.kraftverk.binding.Binding
import io.kraftverk.binding.Value
import io.kraftverk.binding.ValueImpl
import io.kraftverk.binding.XBean
import io.kraftverk.binding.XBeanImpl
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
): XBeanImpl<T, S> = config.let(::process)
    .let { ComponentHandler<T, S>(it) }
    .let(::XBeanImpl)
    .also(this::register)

internal fun <T : Any> Container.createBean(
    config: ComponentDefinition<T, T>
): BeanImpl<T> = config.let(::process)
    .let { ComponentHandler(it) }
    .let(::BeanImpl)
    .also(this::register)

internal fun <T : Any> Container.createValue(
    config: ValueDefinition<T>
): ValueImpl<T> = config.let(::process)
    .let { ValueHandler<T, T>(it) }
    .let(::ValueImpl)
    .also(this::register)

internal fun Container.register(binding: Binding<*>) =
    state.mustBe<State.Configurable> {
        bindings.add(binding)
    }

internal fun Container.start() =
    state.mustBe<State.Configurable> {
        bindings.start()
        state = State.Running(bindings.toList())
        bindings.initialize()
    }

private fun <T : Any, S : Any> Container.process(config: ComponentDefinition<T, S>): ComponentDefinition<T, S> {
    var current = config
    state.mustBe<State.Configurable> {
        for (processor in beanProcessors) {
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
    forEach { binding ->
        when (binding) {
            is Bean<*> -> binding.handler.start()
            is Value<*> -> binding.handler.start()
            is XBean<*, *> -> binding.handler.start()
        }
    }
}

private fun List<Binding<*>>.initialize() {
    val valueNotFoundExceptions = mutableListOf<ValueNotFoundException>()
    filterIsInstance<Value<*>>().forEach { value ->
        try {
            value.handler.initialize()
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
    forEach { binding ->
        when (binding) {
            is XBeanImpl<*, *> -> binding.handler.initialize()
            is Bean<*> -> binding.handler.initialize()
            is Value<*> -> {
                // Already initialized
            }
        }
    }
}
