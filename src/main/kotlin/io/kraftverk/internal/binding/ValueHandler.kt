/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.binding

import io.kraftverk.internal.misc.InstanceFactory
import io.kraftverk.internal.provider.Provider
import kotlin.reflect.KClass
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

internal class ValueHandler<T : Any>(
    private val name: String,
    private val type: KClass<T>,
    private val lazy: Boolean,
    private val secret: Boolean,
    instanceFactory: InstanceFactory<T>
) : BindingHandler<T>(State.Defining(instanceFactory)) {

    override fun createProvider(state: State.Defining<T>) = Provider(
        type = type,
        lazy = lazy,
        createInstance = {
            state.createInstance().also {
                if (secret) {
                    logger.info("Value '$name' is bound to '********'")
                } else {
                    logger.info("Value '$name' is bound to '$it'")
                }
            }
        },
        onCreate = state.onCreate,
        onDestroy = state.onDestroy
    )
}
