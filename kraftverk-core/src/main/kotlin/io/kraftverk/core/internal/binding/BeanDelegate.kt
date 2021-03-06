package io.kraftverk.core.internal.binding

import io.kraftverk.core.declaration.BeanDeclarationContext
import io.kraftverk.core.internal.misc.BasicState
import io.kraftverk.core.internal.misc.Supplier
import io.kraftverk.core.internal.misc.applyAs
import io.kraftverk.core.internal.misc.applyWhen
import io.kraftverk.core.provider.BeanProvider
import io.kraftverk.core.provider.destroy
import io.kraftverk.core.provider.initialize

internal class BeanDelegate<T : Any>(
    providerFactory: BeanProviderFactory<T>
) :
    BindingDelegate<T>() {

    @Volatile
    private var state: State<T> =
        State.Configurable(providerFactory)

    private sealed class State<out T : Any> : BasicState {

        class Configurable<T : Any>(
            val providerFactory: BeanProviderFactory<T>
        ) : State<T>()

        class Running<T : Any>(
            val provider: BeanProvider<T>
        ) : State<T>()

        object Destroyed : State<Nothing>()
    }

    fun bind(block: (BeanDeclarationContext, Supplier<T>) -> T) {
        state.applyAs<State.Configurable<T>> {
            providerFactory.bind(block)
        }
    }

    fun configure(block: (T, BeanDeclarationContext) -> Unit) {
        state.applyAs<State.Configurable<T>> {
            providerFactory.configure(block)
        }
    }

    override fun start() {
        state.applyAs<State.Configurable<T>> {
            val provider = providerFactory.createProvider()
            state = State.Running(provider)
        }
    }

    override fun initialize(lazy: Boolean) {
        state.applyAs<State.Running<T>> {
            provider.initialize(lazy)
        }
    }

    override val provider: BeanProvider<T>
        get() {
            state.applyAs<State.Running<T>> {
                return provider
            }
        }

    override fun stop() {
        state.applyWhen<State.Running<T>> {
            provider.destroy()
            state = State.Destroyed
        }
    }
}
