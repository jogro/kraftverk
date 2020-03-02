package io.kraftverk.internal.binding

import io.kraftverk.internal.misc.InstanceFactory
import kotlin.reflect.KClass
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

internal fun <T : Any> newValueHandler(
    name: String,
    type: KClass<T>,
    lazy: Boolean,
    secret: Boolean,
    createInstance: InstanceFactory<T>
): BindingHandler<T> =
    BindingHandler(
        createInstance,
        createProvider = { create, onCreate, onDestroy ->
            Provider(
                type = type,
                lazy = lazy,
                createInstance = {
                    create().also {
                        if (secret) {
                            logger.info("Value '$name' is bound to '********'")
                        } else {
                            logger.info("Value '$name' is bound to '$it'")
                        }
                    }
                },
                onCreate = onCreate,
                onDestroy = onDestroy
            )
        }
    )
