/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import mu.KotlinLogging
import kotlin.reflect.KClass
import kotlin.time.measureTimedValue

sealed class Binding<out T : Any>

sealed class Bean<out T : Any> : Binding<T>() {
    companion object
}

sealed class Value<out T : Any> : Binding<T>() {
    companion object
}

internal class BeanImpl<T : Any>(val handler: BeanHandler<T>) : Bean<T>()
internal class ValueImpl<T : Any>(val handler: ValueHandler<T>) : Value<T>()

internal fun <T : Any> Binding<T>.onBind(
    block: (() -> T) -> T
) {
    this.toHandler().onBind(block)
}

internal fun <T : Any> Binding<T>.onCreate(
    block: (T, (T) -> Unit) -> Unit
) {
    this.toHandler().onCreate(block)
}

internal fun <T : Any> Binding<T>.onDestroy(
    block: (T, (T) -> Unit) -> Unit
) {
    this.toHandler().onDestroy(block)
}

internal fun Binding<*>.start() {
    this.toHandler().start()
}

internal fun Binding<*>.reset() {
    this.toHandler().reset()
}

internal fun Binding<*>.prepare() {
    this.toHandler().prepare()
}

internal fun Binding<*>.destroy() {
    this.toHandler().destroy()
}

internal fun <T : Any> Binding<T>.provider(): Provider<T> {
    return this.toHandler().provider()
}

private fun <T : Any> Binding<T>.toHandler(): BindingHandler<T> = when (this) {
    is BeanImpl<T> -> handler
    is ValueImpl<T> -> handler
}

private val logger = KotlinLogging.logger {}

internal sealed class BindingHandler<T : Any>(instance: () -> T) {

    @Volatile
    private var state: State<T> =
        State.Defining(instance)

    fun <T : Any> onBind(
        block: (() -> T) -> T
    ) {
        state.applyAs<State.Defining<T>> {
            val supplier = create
            create = {
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
                create,
                onCreate,
                onDestroy
            )
            state = State.Running(provider)
        }
    }

    fun prepare() {
        state.applyAs<State.Running<T>> {
            provider.prepare()
        }
    }

    fun provider(): Provider<T> {
        state.applyAs<State.Running<T>> {
            return provider
        }
    }

    fun reset() {
        state.applyAs<State.Running<T>> {
            provider.reset()
        }
    }

    fun destroy() {
        state.applyWhen<State.Running<T>> {
            provider.destroy()
            state = State.Destroyed
        }
    }

    abstract fun createProvider(
        create: () -> T,
        onCreate: (T) -> Unit = {},
        onDestroy: (T) -> Unit = {}
    ): Provider<T>

    private sealed class State<out T : Any> {

        class Defining<T : Any>(
            instance: () -> T
        ) : State<T>() {
            var create: () -> T = instance
            var onCreate: (T) -> Unit = {}
            var onDestroy: (T) -> Unit = {}
        }

        class Running<T : Any>(val provider: Provider<T>) : State<T>()

        object Destroyed : State<Nothing>()
    }

}

internal class BeanHandler<T : Any>(
    private val name: String,
    private val type: KClass<T>,
    private val lazy: Boolean,
    private val resettable: Boolean,
    createInstance: () -> T
) : BindingHandler<T>(createInstance) {

    override fun createProvider(
        create: () -> T,
        onCreate: (T) -> Unit,
        onDestroy: (T) -> Unit
    ) = Provider(
        type = type,
        lazy = lazy,
        resettable = resettable,
        create = {
            measureTimedValue {
                create()
            }.also {
                logger.info("Bean '$name' is bound to $type (${it.duration})")
            }.value
        },
        onCreate = onCreate,
        onDestroy = onDestroy
    )

}

internal class ValueHandler<T : Any>(
    private val name: String,
    private val type: KClass<T>,
    private val lazy: Boolean,
    private val secret: Boolean,
    createInstance: () -> T
) : BindingHandler<T>(createInstance) {

    override fun createProvider(
        create: () -> T,
        onCreate: (T) -> Unit,
        onDestroy: (T) -> Unit
    ) = Provider(
        type = type,
        lazy = lazy,
        resettable = true,
        create = {
            create().also {
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
