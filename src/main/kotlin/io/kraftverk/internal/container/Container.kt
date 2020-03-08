/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.container

import io.kraftverk.binding.Bean
import io.kraftverk.binding.Binding
import io.kraftverk.binding.Value
import io.kraftverk.binding.handler
import io.kraftverk.binding.provider
import io.kraftverk.env.Environment
import io.kraftverk.internal.misc.applyAs
import io.kraftverk.internal.misc.applyWhen
import io.kraftverk.internal.misc.narrow

internal class Container(
    val lazy: Boolean,
    val environment: Environment
) {

    @Volatile
    private var state: State = State.Defining()

    internal sealed class State {

        class Defining : State() {
            val bindings = mutableListOf<Binding<*>>()
        }

        class Started(
            val bindings: List<Binding<*>>
        ) : State()

        object Destroying : State()
        object Destroyed : State()
    }

    internal fun start() = state.applyAs<State.Defining> {
        bindings.start()
        state = State.Started(bindings.toList())
        bindings.initialize()
    }

    internal fun register(binding: Binding<*>) =
        state.applyAs<State.Defining> {
            bindings.add(binding)
        }

    internal fun checkIsStarted() {
        state.narrow<State.Started>()
    }

    internal fun stop() = state.applyWhen<State.Started> {
        state = State.Destroying
        bindings.destroy()
        state = State.Destroyed
    }
}

private fun List<Binding<*>>.start() {
    forEach { binding ->
        binding.handler.start()
    }
}

private fun List<Binding<*>>.initialize() {
    filterIsInstance<Value<*>>().forEach { value ->
        value.handler.initialize()
    }
    filterIsInstance<Bean<*>>().forEach { bean ->
        bean.handler.initialize()
    }
}

private fun List<Binding<*>>.destroy() {
    filter { binding ->
        binding.provider.instanceId != null
    }.sortedByDescending { binding ->
        binding.provider.instanceId
    }.forEach { binding ->
        binding.handler.stop()
    }
}
