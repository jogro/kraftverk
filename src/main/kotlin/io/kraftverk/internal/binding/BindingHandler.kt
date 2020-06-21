/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.binding

import io.kraftverk.internal.misc.BasicState
import io.kraftverk.provider.Provider

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
}

internal class ComponentHandler<T : Any, S : Any>(providerFactory: ComponentProviderFactory<T, S>) :
    BindingHandler<T, ComponentProviderFactory<T, S>>(providerFactory)

internal class ValueHandler<T : Any>(providerFactory: ValueProviderFactory<T>) :
    BindingHandler<T, ValueProviderFactory<T>>(providerFactory)
