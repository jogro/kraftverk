/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.binding

import io.kraftverk.declaration.LifecycleActions
import io.kraftverk.internal.misc.BasicState
import io.kraftverk.internal.misc.Supplier
import io.kraftverk.internal.misc.mustBe
import io.kraftverk.provider.ComponentProvider
import io.kraftverk.provider.Provider
import io.kraftverk.provider.definition

internal sealed class BindingHandler<T : Any, out F : BindingProviderFactory<T, Provider<T>>>(
    providerFactory: F
) {

    @Volatile
    internal var state: State<T> = State.Configurable(providerFactory)

    internal sealed class State<out T : Any> : BasicState {

        data class Configurable<T : Any, out P : Provider<T>, out F : BindingProviderFactory<T, P>>(
            val providerFactory: F
        ) : State<T>()

        data class Running<T : Any, out P : Provider<T>>(
            val provider: P
        ) : State<T>()

        object Destroyed : State<Nothing>()
    }

    fun bind(
        block: (Supplier<T>) -> T
    ) {
        state.mustBe<State.Configurable<T, Provider<T>, F>> {
            providerFactory.bind(block)
        }
    }

    fun start() {
        state.mustBe<State.Configurable<T, Provider<T>, F>> {
            val provider = providerFactory.createProvider()
            state = State.Running(provider)
        }
    }
}

internal class ComponentHandler<T : Any, S : Any>(providerFactory: ComponentProviderFactory<T, S>) :
    BindingHandler<T, ComponentProviderFactory<T, S>>(providerFactory) {

    fun configure(block: (T, LifecycleActions) -> Unit) {
        state.mustBe<State.Configurable<T, ComponentProvider<T, S>, ComponentProviderFactory<T, S>>> {
            providerFactory.configure(block)
        }
    }

    fun onConfigure(t: T, callback: (S) -> Unit) {
        state.mustBe<State.Running<T, ComponentProvider<T, S>>> {
            provider.definition.onConfigure(t, callback)
        }
    }
}

internal class ValueHandler<T : Any>(providerFactory: ValueProviderFactory<T>) :
    BindingHandler<T, ValueProviderFactory<T>>(providerFactory)
