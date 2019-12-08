/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import mu.KotlinLogging
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

internal sealed class Binding<T : Any>(
    protected val type: KClass<T>,
    initialState: BindingConfiguration<T>
) {
    private var state: BindingState<T> = initialState

    fun <T : Any> onBind(
        block: (() -> T) -> T
    ) {
        state.runAs<BindingConfiguration<T>> {
            val supplier = createInstance
            createInstance = {
                block(supplier)
            }
        }
    }

    fun <T : Any> onCreate(
        block: (T, (T) -> Unit) -> Unit
    ) {
        state.runAs<BindingConfiguration<T>> {
            val consumer = onCreate
            onCreate = { instance ->
                block(instance, consumer)
            }
        }
    }

    fun <T : Any> onDestroy(
        block: (T, (T) -> Unit) -> Unit
    ) {
        state.runAs<BindingConfiguration<T>> {
            val consumer = onDestroy
            onDestroy = { instance ->
                block(instance, consumer)
            }
        }
    }

    fun prepare() {
        state.runAs<BindingConfiguration<T>> {
            state = PreparedBinding(createProvider(this), lazy)
        }
    }

    fun evaluate() {
        state.runAs<PreparedBinding<T>> {
            if (!lazy) provider.instance()
        }
    }

    fun provider(): Provider<T> {
        state.runAs<PreparedBinding<T>> {
            return provider
        }
    }

    fun destroy() {
        state.runIf<PreparedBinding<T>> {
            provider.destroy()
            state = DestroyedBinding
        }
    }

    abstract fun createProvider(state: BindingConfiguration<T>): Provider<T>
}

internal class BeanBinding<T : Any>(
    private val name: String,
    type: KClass<T>,
    initialState: BindingConfiguration<T>
) : Binding<T>(type, initialState) {
    override fun createProvider(state: BindingConfiguration<T>) = with(state) {
        Provider(
            type = type,
            create = {
                val started = System.currentTimeMillis()
                createInstance().also {
                    val elapsed = System.currentTimeMillis() - started
                    logger.info("Bean '$name' is bound to $type (${elapsed}ms)")
                }
            },
            onCreate = onCreate,
            onDestroy = onDestroy
        )
    }
}

internal class PropertyBinding<T : Any>(
    private val name: String,
    private val secret: Boolean,
    type: KClass<T>,
    initialState: BindingConfiguration<T>
) : Binding<T>(type, initialState) {
    override fun createProvider(state: BindingConfiguration<T>) = with(state) {
        Provider(
            type = type,
            create = {
                createInstance().also {
                    if (secret) {
                        logger.info("Property '$name' is bound to '********'")
                    } else {
                        logger.info("Property '$name' is bound to '$it'")
                    }
                }
            },
            onCreate = onCreate,
            onDestroy = onDestroy
        )
    }
}

internal sealed class BindingState<out T : Any>

internal class BindingConfiguration<T : Any>(
    internal val lazy: Boolean,
    instance: () -> T
) : BindingState<T>() {
    var createInstance: () -> T = instance
    var onCreate: (T) -> Unit = {}
    var onDestroy: (T) -> Unit = {}
}

internal class PreparedBinding<T : Any>(val provider: Provider<T>, val lazy: Boolean) :
    BindingState<T>()

internal object DestroyedBinding : BindingState<Nothing>()

internal inline fun <reified T : BindingState<*>> BindingState<*>.runAs(block: T.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    if (this is T) {
        this.block()
    } else {
        throw IllegalStateException("Expected state to be ${T::class} but was ${this::class}")
    }
}

internal inline fun <reified T : BindingState<*>> BindingState<*>.runIf(block: T.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (this is T) {
        this.block()
    }
}
