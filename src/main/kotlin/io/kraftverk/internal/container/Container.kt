/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.container

import io.kraftverk.binding.BeanProcessor
import io.kraftverk.binding.Binding
import io.kraftverk.binding.ValueProcessor
import io.kraftverk.env.Environment
import io.kraftverk.internal.misc.BasicState

internal class Container(
    val lazy: Boolean,
    val environment: Environment,
    beanProcessors: List<BeanProcessor>,
    valueProcessors: List<ValueProcessor>
) {

    @Volatile
    internal var state: State = State.Configurable(
        beanProcessors,
        valueProcessors
    )

    internal sealed class State : BasicState {

        class Configurable(
            val beanProcessors: List<BeanProcessor>,
            val valueProcessors: List<ValueProcessor>
        ) : State() {
            val bindings = mutableListOf<Binding<*>>()
        }

        class Running(
            val bindings: List<Binding<*>>
        ) : State()

        object Destroying : State()
        object Destroyed : State()
    }
}
