/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import mu.KotlinLogging
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

internal sealed class BindingDelegate<T : Any>(
    protected val type: KClass<T>,
    lazy: Boolean,
    instance: () -> T
) {
    private var state: State<T> = State.Defining(lazy, instance)

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

    fun initialize() {
        state.applyAs<State.Defining<T>> {
            val provider = createProvider(
                createInstance,
                onCreate,
                onDestroy
            )
            state = State.Running(provider, lazy)
        }
    }

    fun evaluate() {
        state.applyAs<State.Running<T>> {
            if (!lazy) provider.instance()
        }
    }

    fun provider(): Provider<T> {
        state.applyAs<State.Running<T>> {
            return provider
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
            internal val lazy: Boolean,
            instance: () -> T
        ) : State<T>() {
            var createInstance: () -> T = instance
            var onCreate: (T) -> Unit = {}
            var onDestroy: (T) -> Unit = {}
        }

        class Running<T : Any>(val provider: Provider<T>, val lazy: Boolean) : State<T>()

        object Destroyed : State<Nothing>()
    }

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
