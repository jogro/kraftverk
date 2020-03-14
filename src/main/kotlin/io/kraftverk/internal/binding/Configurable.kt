/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.binding

import io.kraftverk.internal.binding.BindingHandler.State
import io.kraftverk.internal.misc.Consumer
import io.kraftverk.internal.misc.Supplier
import io.kraftverk.internal.misc.intercept
import io.kraftverk.internal.misc.mustBe
import io.kraftverk.provider.Provider

internal fun <T : Any> BindingHandler<T, Provider<T>>.bind(
    block: (Supplier<T>) -> T
) {
    state.mustBe<State.Configurable<T>> {
        instance = intercept(instance, block)
    }
}

internal fun <T : Any> BeanHandler<T>.onCreate(
    block: (T, Consumer<T>) -> Unit
) {
    state.mustBe<State.Configurable<T>> {
        onCreate = intercept(onCreate, block)
    }
}

internal fun <T : Any> BeanHandler<T>.onDestroy(
    block: (T, Consumer<T>) -> Unit
) {
    state.mustBe<State.Configurable<T>> {
        onDestroy = intercept(onDestroy, block)
    }
}

internal fun <T : Any> BindingHandler<T, Provider<T>>.start() {
    state.mustBe<State.Configurable<T>> {
        val provider = createProvider(this)
        state = State.Running(provider)
    }
}
