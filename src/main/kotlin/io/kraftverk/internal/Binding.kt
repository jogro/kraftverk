/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import kotlin.reflect.KClass

internal class Binding<T : Any>(
    internal val type: KClass<T>,
    initialState: DefiningBinding<T>
) {
    private var state: BindingState<T> = initialState

    fun <T : Any> onSupply(
        block: (() -> T) -> T
    ) {
        state.runAs<DefiningBinding<T>> {
            val supplier = onSupply
            onSupply = {
                block(supplier)
            }
        }
    }

    fun <T : Any> onStart(
        block: (T, (T) -> Unit) -> Unit
    ) {
        state.runAs<DefiningBinding<T>> {
            val consumer = onStart
            onStart = { instance ->
                block(instance, consumer)
            }
        }
    }

    fun <T : Any> onStop(
        block: (T, (T) -> Unit) -> Unit
    ) {
        state.runAs<DefiningBinding<T>> {
            val consumer = onStop
            onStop = { instance ->
                block(instance, consumer)
            }
        }
    }

    fun initialize() {
        state.runAs<DefiningBinding<T>> {
            state = InitializedBinding(createProvider(this), lazy)
        }
    }

    fun start() {
        state.runAs<InitializedBinding<T>> {
            if (!lazy) provider.get()
        }
    }

    fun provider(): Provider<T> {
        state.runAs<InitializedBinding<T>> {
            return provider
        }
    }

    fun destroy() {
        state.runIf<InitializedBinding<T>> {
            provider.destroy()
            state = DestroyedBinding
        }
    }

    fun createProvider(state: DefiningBinding<T>): Provider<T> = with(state) {
        return Provider(type, onSupply, onStart, onStop)
    }

}
