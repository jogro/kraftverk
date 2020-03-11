/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.binding

import io.kraftverk.internal.logging.createLogger
import io.kraftverk.internal.provider.Singleton
import io.kraftverk.provider.ValueProviderImpl

internal class ValueHandler<T : Any>(config: BindingConfig<T>) : BindingHandler<T>(config) {

    private val logger = createLogger { }

    override fun createProvider(config: BindingConfig<T>): ValueProviderImpl<T> {
        val configCopy = config.copy().apply {
            val next = instance
            instance = { log(this, next) }
        }
        return ValueProviderImpl(config.name, config.lazy, Singleton.of(configCopy))
    }

    private fun log(config: BindingConfig<T>, block: () -> T): T = block().also {
        if (config.secret) {
            logger.info { "Value '${config.name}' is bound to '********'" }
        } else {
            logger.info { "Value '${config.name}' is bound to '$it'" }
        }
    }
}
