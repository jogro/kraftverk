package io.kraftverk.internal.binding

import io.kraftverk.internal.misc.InstanceFactory
import kotlin.reflect.KClass
import kotlin.time.measureTimedValue
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

internal fun <T : Any> newBeanHandler(
    name: String,
    type: KClass<T>,
    lazy: Boolean,
    createInstance: InstanceFactory<T>
): BindingHandler<T> =
    BindingHandler(
        createInstance,
        createProvider = { create, onCreate, onDestroy ->
            Provider(
                type = type,
                lazy = lazy,
                createInstance = {
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
