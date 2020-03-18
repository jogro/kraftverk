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
import io.kraftverk.common.BeanConfig
import io.kraftverk.common.ValueConfig
import io.kraftverk.internal.binding.BeanHandler
import io.kraftverk.internal.binding.ValueHandler
import io.kraftverk.internal.binding.initialize
import io.kraftverk.internal.binding.start
import io.kraftverk.internal.container.Container.State
import io.kraftverk.internal.misc.mustBe

internal fun <T : Any> Container.createBean(
    config: BeanConfig<T>
): BeanImpl<T> = config.let(::process)
    .let(::BeanHandler)
    .let(::BeanImpl)
    .also(this::register)

internal fun <T : Any> Container.createValue(
    config: ValueConfig<T>
): ValueImpl<T> = config.let(::process)
    .let(::ValueHandler)
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

private fun <T : Any> Container.process(config: BeanConfig<T>): BeanConfig<T> {
    var current = config
    state.mustBe<State.Configurable> {
        for (processor in beanProcessors) {
            current = processor.process(current)
        }
    }
    return current
}

private fun <T : Any> Container.process(config: ValueConfig<T>): ValueConfig<T> {
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
        binding.handler.start()
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
    filterIsInstance<Bean<*>>().forEach { bean ->
        bean.handler.initialize()
    }
}
