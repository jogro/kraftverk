/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.binding

import io.kraftverk.declaration.LifecycleActions
import io.kraftverk.internal.misc.BasicState
import io.kraftverk.internal.misc.Supplier
import io.kraftverk.internal.misc.mightBe
import io.kraftverk.internal.misc.mustBe
import io.kraftverk.provider.ComponentProvider
import io.kraftverk.provider.Provider
import io.kraftverk.provider.ValueProvider
import io.kraftverk.provider.definition
import io.kraftverk.provider.destroy
import io.kraftverk.provider.initialize

internal sealed class BindingHandler<T : Any> {
    abstract fun start()
    abstract fun initialize(lazy: Boolean)
    abstract fun stop()
    abstract val provider: Provider<T>
}

internal class ComponentHandler<T : Any, S : Any>(providerFactory: ComponentProviderFactory<T, S>) :
    BindingHandler<T>() {

    @Volatile
    private var state: State<T> = State.Configurable(providerFactory)

    private sealed class State<out T : Any> : BasicState {

        class Configurable<T : Any, S : Any>(
            val providerFactory: ComponentProviderFactory<T, S>
        ) : State<T>()

        class Running<T : Any, S : Any>(
            val provider: ComponentProvider<T, S>
        ) : State<T>()

        object Destroyed : State<Nothing>()
    }

    fun bind(block: (Supplier<T>) -> T) {
        state.mustBe<State.Configurable<T, S>> {
            providerFactory.bind(block)
        }
    }

    fun setUp(block: (T, LifecycleActions) -> Unit) {
        state.mustBe<State.Configurable<T, S>> {
            providerFactory.setUp(block)
        }
    }

    override fun start() {
        state.mustBe<State.Configurable<T, S>> {
            val provider = providerFactory.createProvider()
            state = State.Running(provider)
        }
    }

    override fun initialize(lazy: Boolean) {
        state.mightBe<State.Running<T, S>> {
            provider.initialize(lazy)
        }
    }

    fun onSetUp(t: T, callback: (S) -> Unit) {
        state.mustBe<State.Running<T, S>> {
            provider.definition.onSetUp(t, callback)
        }
    }

    override val provider: ComponentProvider<T, S>
        get() {
            state.mustBe<State.Running<T, S>> {
                return provider
            }
        }

    override fun stop() {
        state.mightBe<State.Running<T, S>> {
            provider.destroy()
            state = State.Destroyed
        }
    }
}

internal class ValueHandler<T : Any>(providerFactory: ValueProviderFactory<T>) : BindingHandler<T>() {

    @Volatile
    private var state: State<T> = State.Configurable(providerFactory)

    private sealed class State<out T : Any> : BasicState {

        class Configurable<T : Any>(
            val providerFactory: ValueProviderFactory<T>
        ) : State<T>()

        class Running<T : Any>(
            val provider: ValueProvider<T>
        ) : State<T>()

        object Destroyed : State<Nothing>()
    }

    fun bind(block: (Supplier<T>) -> T) {
        state.mustBe<State.Configurable<T>> {
            providerFactory.bind(block)
        }
    }

    override fun start() {
        state.mustBe<State.Configurable<T>> {
            val provider = providerFactory.createProvider()
            state = State.Running(provider)
        }
    }

    override fun initialize(lazy: Boolean) {
        state.mightBe<State.Running<T>> {
            provider.initialize(lazy)
        }
    }

    override val provider: ValueProvider<T>
        get() {
            state.mustBe<State.Running<T>> {
                return provider
            }
        }

    override fun stop() {
        state.mightBe<State.Running<T>> {
            provider.destroy()
            state = State.Destroyed
        }
    }
}
