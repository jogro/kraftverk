/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.binding

import io.kraftverk.internal.binding.BindingHandler.State
import io.kraftverk.internal.misc.Supplier
import io.kraftverk.internal.misc.mustBe
import io.kraftverk.provider.Provider

internal fun <T : Any, P : Provider<T>, F : BindingProviderFactory<T, P>> BindingHandler<T, F>.bind(
    block: (Supplier<T>) -> T
) {
    state.mustBe<State.Configurable<T, P, F>> {
        providerFactory.bind(block)
    }
}

internal fun <T : Any, P : Provider<T>, F : BindingProviderFactory<T, P>> BindingHandler<T, F>.start() {
    state.mustBe<State.Configurable<T, P, F>> {
        val provider = providerFactory.createProvider()
        state = State.Running(provider)
    }
}
