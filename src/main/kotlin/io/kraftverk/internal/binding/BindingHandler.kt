/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.binding

import io.kraftverk.internal.logging.createLogger
import io.kraftverk.internal.misc.BasicState
import io.kraftverk.internal.misc.intercept
import io.kraftverk.internal.provider.Singleton
import io.kraftverk.provider.BeanProviderImpl
import io.kraftverk.provider.Provider
import io.kraftverk.provider.ValueProviderImpl

internal sealed class BindingHandler<T : Any>(config: BindingConfig<T>) {

    @Volatile
    internal var state: State<T> = State.UnderConstruction(config.copy())

    abstract fun createProvider(config: BindingConfig<T>): Provider<T>

    internal sealed class State<out T : Any> : BasicState {

        class UnderConstruction<T : Any>(
            val config: BindingConfig<T>
        ) : State<T>()

        class Running<T : Any>(
            val provider: Provider<T>
        ) : State<T>()

        object Destroyed : State<Nothing>()
    }
}

internal class BeanHandler<T : Any>(config: BindingConfig<T>) : BindingHandler<T>(config) {

    private val logger = createLogger { }

    override fun createProvider(config: BindingConfig<T>): BeanProviderImpl<T> {
        val loggingConfig = createLoggingConfig(config)
        return BeanProviderImpl(
            loggingConfig.name,
            loggingConfig.lazy,
            Singleton.of(loggingConfig)
        )
    }

    private fun createLoggingConfig(config: BindingConfig<T>) =
        config.copy().apply {
            instance = intercept(instance) { proceed ->
                val startMs = System.currentTimeMillis()
                val t = proceed()
                val elapsed = System.currentTimeMillis() - startMs
                logger.info {
                    "Bean '$name' is bound to $$type (${elapsed}ms)"
                }
                t
            }
        }
}

internal class ValueHandler<T : Any>(config: BindingConfig<T>) : BindingHandler<T>(config) {

    private val logger = createLogger { }

    override fun createProvider(config: BindingConfig<T>): ValueProviderImpl<T> {
        val loggingConfig = createLoggingConfig(config)
        return ValueProviderImpl(
            loggingConfig.name,
            loggingConfig.lazy,
            Singleton.of(loggingConfig)
        )
    }

    private fun createLoggingConfig(config: BindingConfig<T>) = config.copy().apply {
        instance = intercept(instance) { proceed ->
            val t = proceed()
            if (secret) {
                logger.info { "Value '$name' is bound to '********'" }
            } else {
                logger.info { "Value '$name' is bound to '$t'" }
            }
            t
        }
    }
}
