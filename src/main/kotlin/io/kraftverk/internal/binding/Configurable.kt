/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.binding

import io.kraftverk.declaration.LifecycleActions
import io.kraftverk.internal.binding.BindingHandler.State
import io.kraftverk.internal.misc.Supplier
import io.kraftverk.internal.misc.interceptAfter
import io.kraftverk.internal.misc.interceptAround
import io.kraftverk.internal.misc.mustBe
import io.kraftverk.provider.Provider

internal fun <T : Any, S : Any> BindingHandler<T, S, Provider<T>>.bind(
    block: (Supplier<T>) -> T
) {
    state.mustBe<State.Configurable<T, S>> {
        instance = interceptAround(instance, block)
    }
}

internal fun <T : Any, S : Any> ComponentHandler<T, S>.onShape(
    block: (T, LifecycleActions) -> Unit
) {
    state.mustBe<State.Configurable<T, S>> {
        onShape = interceptAfter(onShape, block)
    }
}

internal fun <T : Any, S : Any> BindingHandler<T, S, Provider<T>>.start() {
    state.mustBe<State.Configurable<T, S>> {
        val provider = createProvider(this)
        state = State.Running(provider)
    }
}
