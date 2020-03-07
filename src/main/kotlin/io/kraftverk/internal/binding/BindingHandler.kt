/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.binding

import io.kraftverk.internal.misc.Consumer
import io.kraftverk.internal.misc.InstanceFactory
import io.kraftverk.internal.misc.applyAs
import io.kraftverk.internal.misc.applyWhen
import io.kraftverk.internal.provider.Provider

internal abstract class BindingHandler<T : Any>(
    initialState: State.Defining<T>
) {

    @Volatile
    internal var state: State<T> = initialState

    abstract fun createProvider(state: State.Defining<T>): Provider<T>

    internal sealed class State<out T : Any> {

        class Defining<T : Any>(
            instanceFactory: InstanceFactory<T>
        ) : State<T>() {
            var createInstance: InstanceFactory<T> = instanceFactory
            var onCreate: Consumer<T> = {}
            var onDestroy: Consumer<T> = {}
        }

        class Started<T : Any>(
            val provider: Provider<T>
        ) : State<T>()

        object Destroyed : State<Nothing>()
    }
}

internal fun <T : Any> BindingHandler<T>.onBind(
    block: (InstanceFactory<T>) -> T
) {
    state.applyAs<BindingHandler.State.Defining<T>> {
        val next = createInstance
        createInstance = {
            block(next)
        }
    }
}

internal fun <T : Any> BindingHandler<T>.onCreate(
    block: (T, Consumer<T>) -> Unit
) {
    state.applyAs<BindingHandler.State.Defining<T>> {
        val next = onCreate
        onCreate = { instance ->
            block(instance, next)
        }
    }
}

internal fun <T : Any> BindingHandler<T>.onDestroy(
    block: (T, Consumer<T>) -> Unit
) {
    state.applyAs<BindingHandler.State.Defining<T>> {
        val next = onDestroy
        onDestroy = { instance ->
            block(instance, next)
        }
    }
}

internal fun <T : Any> BindingHandler<T>.start() {
    state.applyAs<BindingHandler.State.Defining<T>> {
        val provider = createProvider(this)
        state = BindingHandler.State.Started(provider)
    }
}

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
