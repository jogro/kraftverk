/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.binding

import io.kraftverk.declaration.LifecycleActions
import io.kraftverk.internal.binding.BindingHandler.State
import io.kraftverk.internal.misc.Supplier
import io.kraftverk.internal.misc.mustBe
import io.kraftverk.provider.ComponentProvider
import io.kraftverk.provider.Provider

// TODO Generic parameters can probably be simplified (P)
internal fun <T : Any, P : Provider<T>, F : BindingProviderFactory<T, P>> BindingHandler<T, P, F>.bind(
    block: (Supplier<T>) -> T
) {
    state.mustBe<State.Configurable<T, P, F>> {
        providerFactory.bind(block)
    }
}

internal fun <T : Any, S : Any, F : ComponentProviderFactory<T, S>> ComponentHandler<T, S>.configure(
    block: (T, LifecycleActions) -> Unit
) {
    state.mustBe<State.Configurable<T, ComponentProvider<T, S>, F>> {
        providerFactory.configure(block)
    }
}

internal fun <T : Any, P : Provider<T>, F : BindingProviderFactory<T, P>> BindingHandler<T, P, F>.start() {
    state.mustBe<State.Configurable<T, P, F>> {
        val provider = providerFactory.createProvider()
        state = State.Running(provider)
    }
}
