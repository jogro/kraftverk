/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.binding

import io.kraftverk.common.BeanConfig
import io.kraftverk.common.BindingConfig
import io.kraftverk.common.ValueConfig
import io.kraftverk.internal.logging.createLogger
import io.kraftverk.internal.misc.BasicState
import io.kraftverk.internal.misc.Consumer
import io.kraftverk.internal.misc.Supplier
import io.kraftverk.internal.provider.Singleton
import io.kraftverk.provider.BeanProvider
import io.kraftverk.provider.BeanProviderImpl
import io.kraftverk.provider.Provider
import io.kraftverk.provider.ValueProvider
import io.kraftverk.provider.ValueProviderImpl

internal sealed class BindingHandler<T : Any, out P : Provider<T>>(
    instance: Supplier<T>
) {

    @Volatile
    internal var state: State<T> = State.Configurable(instance)

    abstract fun createProvider(state: State.Configurable<T>): P

    internal sealed class State<out T : Any> : BasicState {

        data class Configurable<T : Any>(
            var instance: Supplier<T>,
            var onCreate: Consumer<T> = { },
            var onDestroy: Consumer<T> = { }
        ) : State<T>()

        data class Running<T : Any, P : Provider<T>>(
            val provider: P
        ) : State<T>()

        object Destroyed : State<Nothing>()
    }
}

internal class BeanHandler<T : Any>(
    val config: BeanConfig<T>
) : BindingHandler<T, BeanProvider<T>>(config.instance) {

    private val logger = createLogger { }

    override fun createProvider(state: State.Configurable<T>) = BeanProviderImpl(
        config,
        createSingleton(
            config,
            state.copy(
                instance = loggingInterceptor(state.instance)
            )
        )
    )

    private fun loggingInterceptor(block: () -> T): () -> T = {
        val startMs = System.currentTimeMillis()
        val t = block()
        val elapsed = System.currentTimeMillis() - startMs
        logger.info {
            "Bean '${config.name}' is bound to ${config.type} (${elapsed}ms)"
        }
        t
    }
}

internal class ValueHandler<T : Any>(
    val config: ValueConfig<T>
) : BindingHandler<T, ValueProvider<T>>(config.instance) {

    private val logger = createLogger { }

    override fun createProvider(state: State.Configurable<T>) = ValueProviderImpl(
        config,
        createSingleton(
            config,
            state.copy(
                instance = loggingInterceptor(state.instance)
            )
        )
    )

    private fun loggingInterceptor(block: () -> T): () -> T = {
        val t = block()
        if (config.secret) {
            logger.info { "Value '${config.name}' is bound to '********'" }
        } else {
            logger.info { "Value '${config.name}' is bound to '$t'" }
        }
        t
    }
}

private fun <T : Any> createSingleton(
    config: BindingConfig<T>,
    state: BindingHandler.State.Configurable<T>
): Singleton<T> = Singleton(
    type = config.type,
    lazy = config.lazy,
    createInstance = state.instance,
    onCreate = state.onCreate,
    onDestroy = state.onDestroy
)
