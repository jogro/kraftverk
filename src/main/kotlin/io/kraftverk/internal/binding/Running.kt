/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.binding

import io.kraftverk.internal.binding.BindingHandler.State
import io.kraftverk.internal.misc.mightBe
import io.kraftverk.internal.misc.mustBe
import io.kraftverk.provider.Provider
import io.kraftverk.provider.destroy
import io.kraftverk.provider.initialize

internal fun BindingHandler<*, Provider<*>>.initialize(lazy: Boolean) =
    state.mustBe<State.Running<*, *>> {
        provider.initialize(lazy)
    }

internal val <T : Any, P : Provider<T>> BindingHandler<T, P>.provider: P
    get() {
        state.mustBe<State.Running<T, P>> {
            return provider
        }
    }

internal fun BindingHandler<*, *>.stop() =
    state.mightBe<State.Running<*, *>> {
        provider.destroy()
        state = State.Destroyed
    }
