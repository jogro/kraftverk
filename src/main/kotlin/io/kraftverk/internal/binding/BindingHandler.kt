/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.binding

import io.kraftverk.internal.misc.Consumer
import io.kraftverk.internal.misc.InstanceFactory
import io.kraftverk.internal.misc.applyAs
import io.kraftverk.internal.misc.applyWhen
import io.kraftverk.provider.Provider
import io.kraftverk.provider.destroy
import io.kraftverk.provider.initialize

internal abstract class BindingHandler<T : Any>(config: BindingConfig<T>) {

    @Volatile
    internal var state: State<T> = State.Defining(config.copy())

    abstract fun createProvider(config: BindingConfig<T>): Provider<T>

    internal sealed class State<out T : Any> {

        class Defining<T : Any>(
            val config: BindingConfig<T>
        ) : State<T>()

        class Started<T : Any>(
            val provider: Provider<T>
        ) : State<T>()

        object Destroyed : State<Nothing>()
    }

    internal fun onBind(
        block: (InstanceFactory<T>) -> T
    ) {
        state.applyAs<State.Defining<T>> {
            val next = config.instance
            config.instance = {
                block(next)
            }
        }
    }

    internal fun onCreate(
        block: (T, Consumer<T>) -> Unit
    ) {
        state.applyAs<State.Defining<T>> {
            val next = config.onCreate
            config.onCreate = { instance ->
                block(instance, next)
            }
        }
    }

    internal fun onDestroy(
        block: (T, Consumer<T>) -> Unit
    ) {
        state.applyAs<State.Defining<T>> {
            val next = config.onDestroy
            config.onDestroy = { instance ->
                block(instance, next)
            }
        }
    }

    internal fun start() {
        state.applyAs<State.Defining<T>> {
            val provider = createProvider(config)
            state = State.Started(provider)
        }
    }

    internal fun initialize() =
        state.applyAs<State.Started<*>> {
            provider.initialize()
        }

    internal val provider: Provider<T>
        get() {
            state.applyAs<State.Started<T>> {
                return provider
            }
        }

    internal fun stop() =
        state.applyWhen<State.Started<*>> {
            provider.destroy()
            state = State.Destroyed
        }
}
