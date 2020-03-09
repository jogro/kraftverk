/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.binding

import io.kraftverk.internal.misc.InstanceFactory
import io.kraftverk.internal.provider.Singleton
import io.kraftverk.provider.BeanProviderImpl
import kotlin.reflect.KClass
import kotlin.time.measureTimedValue
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

internal class BeanHandler<T : Any>(
    private val name: String,
    private val type: KClass<T>,
    private val lazy: Boolean,
    instanceFactory: InstanceFactory<T>
) : BindingHandler<T>(State.Defining(instanceFactory)) {

    override fun createProvider(state: State.Defining<T>): BeanProviderImpl<T> =
        BeanProviderImpl(name, lazy, createSingleton(state))

    private fun createSingleton(
        state: State.Defining<T>
    ) = Singleton(
        type = type,
        lazy = lazy,
        createInstance = {
            measureTimedValue {
                state.createInstance()
            }.also {
                logger.info("Bean '$name' is bound to $type (${it.duration})")
            }.value
        },
        onCreate = state.onCreate,
        onDestroy = state.onDestroy
    )
}
