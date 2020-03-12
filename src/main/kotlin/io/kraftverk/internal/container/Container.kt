/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.container

import io.kraftverk.binding.Binding
import io.kraftverk.env.Environment

internal class Container(
    val lazy: Boolean,
    val environment: Environment
) {

    @Volatile
    internal var state: State = State.UnderConstruction()

    internal sealed class State {

        class UnderConstruction : State() {
            val bindings = mutableListOf<Binding<*>>()
        }

        class Started(
            val bindings: List<Binding<*>>
        ) : State()

        object Destroying : State()
        object Destroyed : State()
    }
}
