/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import mu.KotlinLogging
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

internal sealed class Binding<T : Any>(
    protected val type: KClass<T>,
    initialState: DefiningBinding<T>
) {
    protected var state: BindingState<T> = initialState

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

    fun bind() {
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

    abstract fun createProvider(state: DefiningBinding<T>): Provider<T>
}

internal class BeanBinding<T : Any>(
    private val name: String,
    type: KClass<T>,
    initialState: DefiningBinding<T>
) : Binding<T>(type, initialState) {
    override fun createProvider(state: DefiningBinding<T>) = with(state) {
        Provider(
            type = type,
            onCreate = {
                onSupply().also {
                    logger.info("Bean '$name' is bound to $type")
                }
            },
            onStart = onStart,
            onDestroy = onStop
        )
    }
}

internal class PropertyBinding<T : Any>(
    private val name: String,
    private val secret: Boolean,
    type: KClass<T>,
    initialState: DefiningBinding<T>
) : Binding<T>(type, initialState) {
    override fun createProvider(state: DefiningBinding<T>) = with(state) {
        Provider(
            type = type,
            onCreate = {
                onSupply().also {
                    if (secret) {
                        logger.info("Property '$name' is bound to '********'")
                    } else {
                        logger.info("Property '$name' is bound to '$it'")
                    }
                }
            },
            onStart = onStart,
            onDestroy = onStop
        )
    }
}
