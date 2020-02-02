/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass
import kotlin.time.measureTimedValue

sealed class Binding<out T : Any>

sealed class Bean<out T : Any> : Binding<T>() {
    companion object
}

sealed class Value<out T : Any> : Binding<T>() {
    companion object
}

internal class BeanImpl<T : Any>(val handler: BindingHandler<T>) : Bean<T>()
internal class ValueImpl<T : Any>(val handler: BindingHandler<T>) : Value<T>()

internal fun <T : Any> Binding<T>.onBind(block: (() -> T) -> T) = handler.onBind(block)
internal fun <T : Any> Binding<T>.onCreate(block: (T, (T) -> Unit) -> Unit) = handler.onCreate(block)
internal fun <T : Any> Binding<T>.onDestroy(block: (T, (T) -> Unit) -> Unit) = handler.onDestroy(block)
internal fun Binding<*>.start() = handler.start()
internal fun Binding<*>.reset() = handler.reset()
internal fun Binding<*>.initialize() = handler.initialize()
internal fun Binding<*>.destroy() = handler.destroy()
internal val <T : Any> Binding<T>.provider get() = handler.provider

private val <T : Any> Binding<T>.handler: BindingHandler<T>
    get() = when (this) {
        is BeanImpl<T> -> handler
        is ValueImpl<T> -> handler
    }

private val logger = KotlinLogging.logger {}

internal typealias ProviderFactory<T> = (
    create: () -> T,
    onCreate: (T) -> Unit,
    onDestroy: (T) -> Unit
) -> Provider<T>

internal class BindingHandler<T : Any>(createInstance: () -> T, val createProvider: ProviderFactory<T>) {

    @Volatile
    private var state: State<T> =
        State.Defining(createInstance)

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

    fun initialize() {
        state.applyAs<State.Running<T>> {
            provider.initialize()
        }
    }

    val provider: Provider<T>
        get() {
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

internal fun <T : Any> newBeanHandler(
    name: String,
    type: KClass<T>,
    lazy: Boolean,
    resettable: Boolean,
    createInstance: () -> T
): BindingHandler<T> {
    return BindingHandler(
        createInstance,
        createProvider = { create, onCreate, onDestroy ->
            Provider(
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
    )
}

internal fun <T : Any> newValueHandler(
    name: String,
    type: KClass<T>,
    lazy: Boolean,
    secret: Boolean,
    createInstance: () -> T
): BindingHandler<T> {

    return BindingHandler(
        createInstance,
        createProvider = { create, onCreate, onDestroy ->
            Provider(
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
    )
}

internal class Provider<T : Any>(
    val type: KClass<T>,
    val lazy: Boolean,
    val resettable: Boolean,
    private val create: () -> T,
    private val onCreate: (T) -> Unit,
    private val onDestroy: (T) -> Unit
) {

    @Volatile
    private var instance: Instance<T>? = null

    val instanceId: Int?
        get() = synchronized(this) {
            instance?.id
        }

    fun initialize() {
        if (!lazy) get()
    }

    fun get(): T {
        val i = instance
        if (i != null) {
            return i.value
        }
        return synchronized(this) {
            val i2 = instance
            if (i2 != null) {
                i2.value
            } else {
                val i3 = Instance(
                    create(),
                    currentInstanceId.incrementAndGet()
                )
                onCreate(i3.value)
                instance = i3
                i3.value
            }
        }
    }

    fun reset() {
        if (resettable) destroy()
    }

    fun destroy() {
        synchronized(this) {
            val i = instance
            if (i != null) {
                try {
                    onDestroy(i.value)
                } catch (ex: Exception) {
                    logger.error("Couldn't destroy bean", ex)
                }
                instance = null
            }
        }
    }

    companion object {
        val currentInstanceId = AtomicInteger()
    }

    data class Instance<T : Any>(val value: T, val id: Int)

}


