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

internal sealed class BindingDelegate<T : Any>(
    protected val type: KClass<T>,
    lazy: Boolean,
    instance: () -> T
) {
    private var state: BindingDelegateState<T> = BindingDelegateState.Configuring(lazy, instance)

    fun <T : Any> onBind(
        block: (() -> T) -> T
    ) {
        state.runAs<BindingDelegateState.Configuring<T>> {
            val supplier = createInstance
            createInstance = {
                block(supplier)
            }
        }
    }

    fun <T : Any> onCreate(
        block: (T, (T) -> Unit) -> Unit
    ) {
        state.runAs<BindingDelegateState.Configuring<T>> {
            val consumer = onCreate
            onCreate = { instance ->
                block(instance, consumer)
            }
        }
    }

    fun <T : Any> onDestroy(
        block: (T, (T) -> Unit) -> Unit
    ) {
        state.runAs<BindingDelegateState.Configuring<T>> {
            val consumer = onDestroy
            onDestroy = { instance ->
                block(instance, consumer)
            }
        }
    }

    fun start() {
        state.runAs<BindingDelegateState.Configuring<T>> {
            val provider = createProvider(
                createInstance,
                onCreate,
                onDestroy
            )
            state = BindingDelegateState.Running(provider)
            if (!lazy) provider.instance()
        }
    }

    fun provider(): Provider<T> {
        state.runAs<BindingDelegateState.Running<T>> {
            return provider
        }
    }

    fun destroy() {
        state.runIf<BindingDelegateState.Running<T>> {
            provider.destroy()
            state = BindingDelegateState.Destroyed
        }
    }

    abstract fun createProvider(
        createInstance: () -> T,
        onCreate: (T) -> Unit = {},
        onDestroy: (T) -> Unit = {}
    ): Provider<T>

}

internal class BeanDelegate<T : Any>(
    private val name: String,
    type: KClass<T>,
    lazy: Boolean,
    instance: () -> T
) : BindingDelegate<T>(type, lazy, instance) {

    override fun createProvider(
        createInstance: () -> T,
        onCreate: (T) -> Unit,
        onDestroy: (T) -> Unit
    ) =
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

internal class PropertyDelegate<T : Any>(
    private val name: String,
    private val secret: Boolean,
    type: KClass<T>,
    lazy: Boolean,
    instance: () -> T
) : BindingDelegate<T>(type, lazy, instance) {

    override fun createProvider(
        createInstance: () -> T,
        onCreate: (T) -> Unit,
        onDestroy: (T) -> Unit
    ) =
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


private sealed class BindingDelegateState<out T : Any> {

    class Configuring<T : Any>(
        internal val lazy: Boolean,
        instance: () -> T
    ) : BindingDelegateState<T>() {
        var createInstance: () -> T = instance
        var onCreate: (T) -> Unit = {}
        var onDestroy: (T) -> Unit = {}
    }

    class Running<T : Any>(val provider: Provider<T>) : BindingDelegateState<T>()

    object Destroyed : BindingDelegateState<Nothing>()
}

private inline fun <reified T : BindingDelegateState<*>> BindingDelegateState<*>.runAs(block: T.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    if (this is T) {
        this.block()
    } else {
        throw IllegalStateException("Expected state to be ${T::class} but was ${this::class}")
    }
}

private inline fun <reified T : BindingDelegateState<*>> BindingDelegateState<*>.runIf(block: T.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (this is T) {
        this.block()
    }
}
