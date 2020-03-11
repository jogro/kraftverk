/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.binding

import io.kraftverk.internal.logging.createLogger
import io.kraftverk.internal.provider.Singleton
import io.kraftverk.provider.BeanProviderImpl

internal class BeanHandler<T : Any>(config: BindingConfig<T>) : BindingHandler<T>(config) {

    private val logger = createLogger { }

    override fun createProvider(config: BindingConfig<T>): BeanProviderImpl<T> {
        val configCopy = config.copy().apply {
            val next = instance
            instance = { log(this, next) }
        }
        return BeanProviderImpl(config.name, config.lazy, Singleton.of(configCopy))
    }

    private fun log(config: BindingConfig<T>, block: () -> T): T {
        val startMs = System.currentTimeMillis()
        return block().also {
            logger.info {
                val elapsed = System.currentTimeMillis() - startMs
                "Bean '${config.name}' is bound to $${config.type} (${elapsed}ms)"
            }
        }
    }
}
