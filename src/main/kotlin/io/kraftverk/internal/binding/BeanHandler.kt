/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.binding

import io.kraftverk.internal.logging.createLogger
import io.kraftverk.internal.misc.intercept
import io.kraftverk.internal.provider.Singleton
import io.kraftverk.provider.BeanProviderImpl

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
