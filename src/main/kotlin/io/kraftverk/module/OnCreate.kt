package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.handler
import io.kraftverk.definition.BeanConsumerDefinition
import io.kraftverk.internal.binding.onCreate

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
