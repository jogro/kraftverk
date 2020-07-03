package io.kraftverk.core.internal.binding

import io.kraftverk.core.internal.misc.BasicState
import io.kraftverk.core.internal.misc.Supplier
import io.kraftverk.core.internal.misc.mightBe
import io.kraftverk.core.internal.misc.mustBe
import io.kraftverk.core.provider.ValueProvider
import io.kraftverk.core.provider.destroy
import io.kraftverk.core.provider.initialize

internal class ValueDelegate<T : Any>(providerFactory: ValueProviderFactory<T>) : BindingDelegate<T>() {

    @Volatile
    private var state: State<T> =
        State.Configurable(providerFactory)

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
        state.mustBe<State.Running<T>> {
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