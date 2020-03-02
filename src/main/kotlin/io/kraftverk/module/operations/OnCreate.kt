package io.kraftverk.module.operations

import io.kraftverk.binding.Bean
import io.kraftverk.binding.handler
import io.kraftverk.definition.BeanConsumerDefinition
import io.kraftverk.internal.binding.onCreate
import io.kraftverk.module.Module

fun <T : Any> Module.onCreate(
    bean: Bean<T>,
    block: BeanConsumerDefinition<T>.(T) -> Unit
) {
    bean.handler.onCreate { instance, consumer ->
        BeanConsumerDefinition(
            container.environment,
            instance,
            consumer
        ).block(instance)
    }
}
