/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.binding

import io.kraftverk.internal.misc.applyAs
import io.kraftverk.internal.misc.applyWhen
import io.kraftverk.provider.Provider
import io.kraftverk.provider.destroy
import io.kraftverk.provider.initialize

internal fun BindingHandler<*>.initialize() =
    state.applyAs<BindingHandler.State.Started<*>> {
        provider.initialize()
    }

internal val <T : Any> BindingHandler<T>.provider: Provider<T>
    get() {
        state.applyAs<BindingHandler.State.Started<T>> {
            return provider
        }
    }

internal fun BindingHandler<*>.stop() =
    state.applyWhen<BindingHandler.State.Started<*>> {
        provider.destroy()
        state = BindingHandler.State.Destroyed
    }
