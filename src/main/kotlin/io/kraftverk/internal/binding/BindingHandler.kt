/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.binding

import io.kraftverk.internal.logging.createLogger
import io.kraftverk.internal.misc.BasicState
import io.kraftverk.internal.misc.Consumer
import io.kraftverk.internal.misc.Supplier
import io.kraftverk.internal.misc.intercept
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
    internal var state: State<T> = State.UnderConstruction(instance)

    abstract fun createProvider(state: State.UnderConstruction<T>): P

    internal sealed class State<out T : Any> : BasicState {

        data class UnderConstruction<T : Any>(
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

    override fun createProvider(state: State.UnderConstruction<T>) = BeanProviderImpl(
        config.name,
        config.lazy,
        createSingleton(
            config,
            state.copy(
                instance = loggingInterceptor(state.instance)
            )
        )
    )

    private fun loggingInterceptor(block: () -> T) = intercept(block) { proceed ->
        val startMs = System.currentTimeMillis()
        val t = proceed()
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

    override fun createProvider(state: State.UnderConstruction<T>) = ValueProviderImpl(
        config.name,
        config.lazy,
        createSingleton(
            config,
            state.copy(
                instance = loggingInterceptor(state.instance)
            )
        )
    )

    private fun loggingInterceptor(block: () -> T) = intercept(block) { proceed ->
        val t = proceed()
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
    state: BindingHandler.State.UnderConstruction<T>
): Singleton<T> = Singleton(
    type = config.type,
    lazy = config.lazy,
    createInstance = state.instance,
    onCreate = state.onCreate,
    onDestroy = state.onDestroy
)
