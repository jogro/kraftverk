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
import io.kraftverk.binding.provider
import io.kraftverk.definition.BeanDefinition
import io.kraftverk.definition.ValueDefinition
import io.kraftverk.env.Environment
import io.kraftverk.env.get
import io.kraftverk.internal.binding.destroy
import io.kraftverk.internal.binding.initialize
import io.kraftverk.internal.binding.newBeanHandler
import io.kraftverk.internal.binding.newValueHandler
import io.kraftverk.internal.binding.start
import io.kraftverk.internal.component.BeanConfig
import io.kraftverk.internal.component.ValueConfig
import io.kraftverk.internal.misc.applyAs
import io.kraftverk.internal.misc.applyWhen
import io.kraftverk.internal.misc.narrow

internal class Container(
    val lazy: Boolean,
    val environment: Environment
) {

    @Volatile
    internal var state: State =
        State.Defining()

    internal sealed class State {

        class Defining : State() {
            val bindings = mutableListOf<Binding<*>>()
        }

        class Running(
            val bindings: List<Binding<*>>
        ) : State()

        object Destroying : State()
        object Destroyed : State()
    }
}

internal fun <T : Any> Container.newBean(
    name: String,
    config: BeanConfig<T>
) = BeanImpl(
    newBeanHandler(
        name = name,
        type = config.type,
        lazy = config.lazy ?: lazy,
        createInstance = {
            state.checkIsRunning()
            config.createInstance(BeanDefinition(environment))
        }
    )
).apply(::register)

internal fun <T : Any> Container.newValue(
    name: String,
    config: ValueConfig<T>
) = ValueImpl(
    newValueHandler(
        name = name,
        secret = config.secret,
        type = config.type,
        lazy = config.lazy ?: lazy,
        createInstance = {
            state.checkIsRunning()
            config.createInstance(
                ValueDefinition(environment),
                environment[name] ?: config.default ?: throwValueNotFound(name)
            )
        }
    )
).apply(::register)

internal fun Container.start() =
    state.applyAs<Container.State.Defining> {
        state = Container.State.Running(bindings.toList())
        bindings.forEach { binding ->
            binding.handler.start()
        }
        bindings.filterIsInstance<Value<*>>().forEach { value ->
            value.handler.initialize()
        }
        bindings.filterIsInstance<Bean<*>>().forEach { bean ->
            bean.handler.initialize()
        }
    }

internal fun Container.stop() =
    state.applyWhen<Container.State.Running> {
        state = Container.State.Destroying
        bindings.filter { binding ->
            binding.provider.instanceId != null
        }.sortedByDescending { binding ->
            binding.provider.instanceId
        }.forEach { binding ->
            binding.handler.destroy()
        }
        state = Container.State.Destroyed
    }

private fun Container.State.checkIsRunning() {
    narrow<Container.State.Running>()
}

private fun Container.register(binding: Binding<*>) =
    state.applyAs<Container.State.Defining> {
        bindings.add(binding)
    }

private fun throwValueNotFound(name: String): Nothing =
    throw IllegalStateException("Value '$name' was not found!")
