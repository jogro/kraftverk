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

internal fun <T : Any> BindingHandler<T, Provider<T>>.bind(
    block: (Supplier<T>) -> T
) {
    state.mustBe<State.Configurable<T>> {
        instance = interceptAround(instance, block)
    }
}

internal fun <T : Any, S : Any> ComponentHandler<T, S>.onConfigure(
    block: (T, LifecycleActions) -> Unit
) {
    state.mustBe<State.Configurable<T>> {
        onConfigure = interceptAfter(onConfigure, block)
    }
}

internal fun <T : Any> BindingHandler<T, Provider<T>>.start() {
    state.mustBe<State.Configurable<T>> {
        val provider = createProvider(this)
        state = State.Running(provider)
    }
}
