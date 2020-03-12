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

internal fun BindingHandler<*, Provider<*>>.initialize() =
    state.mustBe<State.Running<*>> {
        provider.initialize()
    }

internal val <T : Any> BindingHandler<T, Provider<T>>.provider: Provider<T>
    get() {
        state.mustBe<State.Running<T>> {
            return provider
        }
    }

internal fun BindingHandler<*, Provider<*>>.stop() =
    state.mightBe<State.Running<*>> {
        provider.destroy()
        state = State.Destroyed
    }
