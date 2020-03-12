/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.binding

import io.kraftverk.internal.binding.BindingHandler.State
import io.kraftverk.internal.misc.Consumer
import io.kraftverk.internal.misc.InstanceFactory
import io.kraftverk.internal.misc.applyAs
import io.kraftverk.internal.misc.intercept

internal fun <T : Any> BindingHandler<T>.bind(
    block: (InstanceFactory<T>) -> T
) {
    state.applyAs<State.UnderConstruction<T>> {
        config.instance = intercept(config.instance, block)
    }
}

internal fun <T : Any> BindingHandler<T>.onCreate(
    block: (T, Consumer<T>) -> Unit
) {
    state.applyAs<State.UnderConstruction<T>> {
        config.onCreate = intercept(config.onCreate, block)
    }
}

internal fun <T : Any> BindingHandler<T>.onDestroy(
    block: (T, Consumer<T>) -> Unit
) {
    state.applyAs<State.UnderConstruction<T>> {
        config.onDestroy = intercept(config.onDestroy, block)
    }
}

internal fun <T : Any> BindingHandler<T>.start() {
    state.applyAs<State.UnderConstruction<T>> {
        val provider = createProvider(config)
        state = State.Started(provider)
    }
}
