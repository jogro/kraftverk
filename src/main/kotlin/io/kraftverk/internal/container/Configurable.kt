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
import io.kraftverk.binding.delegate
import io.kraftverk.common.BeanDefinition
import io.kraftverk.common.ValueDefinition
import io.kraftverk.declaration.LifecycleActions
import io.kraftverk.internal.container.Container.State
import io.kraftverk.internal.misc.mustBe

internal fun <T : Any> Container.createBean(
    definition: BeanDefinition<T>,
    lifecycleActions: LifecycleActions
): BeanImpl<T> {
    state.mustBe<State.Configurable> {
        return beanFactory.createBean(definition, lifecycleActions).also { bindings.add(it) }
    }
}

internal fun <T : Any> Container.createValue(
    definition: ValueDefinition<T>
): ValueImpl<T> {
    state.mustBe<State.Configurable> {
        return valueFactory.createValue(definition).also { bindings.add(it) }
    }
}

internal fun Container.configure(block: () -> Unit) =
    state.mustBe<State.Configurable> {
        val previous = onConfigure
        onConfigure = {
            previous()
            block()
        }
    }

internal fun Container.start(lazy: Boolean) =
    state.mustBe<State.Configurable> {
        onConfigure()
        bindings.forEach { binding -> binding.delegate.start() }
        state = State.Running(bindings.toList())
        bindings.initialize(lazy)
    }

private fun List<Binding<*>>.initialize(lazy: Boolean) {
    val valueNotFoundExceptions = mutableListOf<ValueNotFoundException>()
    filterIsInstance<Value<*>>().forEach { value ->
        try {
            value.delegate.initialize(lazy)
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
    filterIsInstance<Bean<*>>().forEach { bean -> bean.delegate.initialize(lazy) }
}
