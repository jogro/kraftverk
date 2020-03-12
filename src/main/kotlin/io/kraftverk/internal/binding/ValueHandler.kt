/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.binding

import io.kraftverk.internal.logging.createLogger
import io.kraftverk.internal.misc.intercept
import io.kraftverk.internal.provider.Singleton
import io.kraftverk.provider.ValueProviderImpl

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
