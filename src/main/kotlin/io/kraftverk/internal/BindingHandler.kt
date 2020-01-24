/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import mu.KotlinLogging
import kotlin.reflect.KClass
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger {}

internal sealed class BindingHandler<T : Any>(
    val container: Container,
    protected val type: KClass<T>,
    lazy: Boolean,
    refreshable: Boolean,
    instance: () -> T
) {
    @Volatile
    private var state: State<T> = State.Defining(lazy, refreshable, instance)

    fun <T : Any> onBind(
        block: (() -> T) -> T
    ) {
        state.applyAs<State.Defining<T>> {
            val supplier = createInstance
            createInstance = {
                block(supplier)
            }
        }
    }

    fun <T : Any> onCreate(
        block: (T, (T) -> Unit) -> Unit
    ) {
        state.applyAs<State.Defining<T>> {
            val consumer = onCreate
            onCreate = { instance ->
                block(instance, consumer)
            }
        }
    }

    fun <T : Any> onDestroy(
        block: (T, (T) -> Unit) -> Unit
    ) {
        state.applyAs<State.Defining<T>> {
            val consumer = onDestroy
            onDestroy = { instance ->
                block(instance, consumer)
            }
        }
    }

    fun start() {
        state.applyAs<State.Defining<T>> {
            val provider = createProvider(
                createInstance,
                onCreate,
                onDestroy
            )
            state = State.Running(provider, lazy, refreshable)
        }
    }

    fun prepare() {
        state.applyAs<State.Running<T>> {
            if (!lazy) provider.instance()
        }
    }

    fun provider(): Provider<T> {
        state.applyAs<State.Running<T>> {
            return provider
        }
    }

    fun refresh() {
        state.applyAs<State.Running<T>> {
            if (refreshable) {
                provider.destroy()
            }
        }
    }

    fun destroy() {
        state.applyWhen<State.Running<T>> {
            provider.destroy()
            state = State.Destroyed
        }
    }

    abstract fun createProvider(
        createInstance: () -> T,
        onCreate: (T) -> Unit = {},
        onDestroy: (T) -> Unit = {}
    ): Provider<T>

    private sealed class State<out T : Any> {

        class Defining<T : Any>(
            val lazy: Boolean,
            val refreshable: Boolean,
            instance: () -> T
        ) : State<T>() {
            var createInstance: () -> T = instance
            var onCreate: (T) -> Unit = {}
            var onDestroy: (T) -> Unit = {}
        }

        class Running<T : Any>(val provider: Provider<T>, val lazy: Boolean, val refreshable: Boolean) : State<T>()

        object Destroyed : State<Nothing>()
    }

}

internal class BeanHandler<T : Any>(
    container: Container,
    private val name: String,
    type: KClass<T>,
    lazy: Boolean,
    refreshable: Boolean,
    instance: () -> T
) : BindingHandler<T>(container, type, lazy, refreshable, instance) {

    override fun createProvider(
        createInstance: () -> T,
        onCreate: (T) -> Unit,
        onDestroy: (T) -> Unit
    ) = Provider(
        container = container,
        type = type,
        create = {
            measureTimedValue {
                createInstance()
            }.also {
                logger.info("Bean '$name' is bound to $type (${it.duration})")
            }.value
        },
        onCreate = onCreate,
        onDestroy = onDestroy
    )

}

internal class ValueHandler<T : Any>(
    container: Container,
    private val name: String,
    private val secret: Boolean,
    type: KClass<T>,
    lazy: Boolean,
    instance: () -> T
) : BindingHandler<T>(container, type, lazy, true, instance) {

    override fun createProvider(
        createInstance: () -> T,
        onCreate: (T) -> Unit,
        onDestroy: (T) -> Unit
    ) = Provider(
        container = container,
        type = type,
        create = {
            createInstance().also {
                if (secret) {
                    logger.info("Value '$name' is bound to '********'")
                } else {
                    logger.info("Value '$name' is bound to '$it'")
                }
            }
        },
        onCreate = onCreate,
        onDestroy = onDestroy
    )
}