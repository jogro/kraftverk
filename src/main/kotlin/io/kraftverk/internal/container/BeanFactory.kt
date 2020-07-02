package io.kraftverk.internal.container

import io.kraftverk.binding.BeanImpl
import io.kraftverk.common.BeanDefinition
import io.kraftverk.common.BeanProcessor
import io.kraftverk.declaration.LifecycleActions
import io.kraftverk.internal.binding.BeanDelegate
import io.kraftverk.internal.binding.BeanProviderFactory

internal class BeanFactory(private val beanProcessors: List<BeanProcessor>) {

    fun <T : Any> createBean(definition: BeanDefinition<T>, lifecycleActions: LifecycleActions): BeanImpl<T> =
        definition.let(::process)
            .let { BeanProviderFactory(it, lifecycleActions) }
            .let(::BeanDelegate)
            .let(::BeanImpl)

    private fun <T : Any> process(definition: BeanDefinition<T>): BeanDefinition<T> {
        var current = definition
        for (processor in beanProcessors) {
            current = processor.process(current)
        }
        return current
    }
}
