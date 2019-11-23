/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.ConsumerDefinition
import io.kraftverk.SupplierDefinition
import kotlin.reflect.KClass

internal sealed class Binding<T : Any, out P : Provider<T>>(
    internal val type: KClass<T>,
    initialState: DefiningBinding<T>
) {
    private var state: BindingState<T> = initialState

    fun <T : Any> onSupply(
        appContext: AppContext,
        block: SupplierDefinition<T>.() -> T
    ) {
        state.expect<DefiningBinding<T>> {
            val supplier = onSupply
            onSupply = {
                define(appContext, supplier, block)
            }
        }
    }

    fun <T : Any> onStart(
        appContext: AppContext,
        block: ConsumerDefinition<T>.(T) -> Unit
    ) {
        state.expect<DefiningBinding<T>> {
            val consumer = onStart
            onStart = { instance ->
                define(appContext, instance, consumer, block)
            }
        }
    }

    fun <T : Any> onStop(
        appContext: AppContext,
        block: ConsumerDefinition<T>.(T) -> Unit
    ) {
        state.expect<DefiningBinding<T>> {
            val consumer = onStop
            onStop = { instance ->
                define(appContext, instance, consumer, block)
            }
        }
    }

    fun initialize() {
        state.expect<DefiningBinding<T>> {
            state = InitializedBinding(createProvider(this), lazy)
        }
    }

    fun start() {
        state.expect<InitializedBinding<T, P>> {
            if (!lazy) provider.get()
        }
    }

    open fun provider(): P {
        state.expect<InitializedBinding<T, P>> {
            return provider
        }
    }

    fun destroy() {
        state.on<InitializedBinding<T, P>> {
            provider.destroy()
            state = DestroyedBinding
        }
    }

    abstract fun createProvider(state: DefiningBinding<T>): Provider<T>

    private fun <T : Any> define(
        appContext: AppContext,
        supplier: () -> T,
        block: SupplierDefinition<T>.() -> T
    ) =
        SupplierDefinition(appContext, supplier).block()

    private fun <T : Any> define(
        appContext: AppContext,
        instance: T,
        consumer: (T) -> Unit,
        block: ConsumerDefinition<T>.(T) -> Unit
    ) =
        ConsumerDefinition(appContext, instance, consumer).block(instance)

}

internal class SingletonBinding<T : Any>(
    type: KClass<T>,
    initialState: DefiningBinding<T>
) : Binding<T, SingletonProvider<T>>(type, initialState) {
    override fun createProvider(state: DefiningBinding<T>): Provider<T> {
        with(state) {
            return SingletonProvider(type, onSupply, onStart, onStop)
        }
    }
}

internal class PropertyBinding(
    initialState: DefiningBinding<String>
) : Binding<String, SingletonProvider<String>>(String::class, initialState) {
    override fun createProvider(state: DefiningBinding<String>): SingletonProvider<String> {
        with(state) {
            return SingletonProvider(type, onSupply, onStart, onStop)
        }
    }
}