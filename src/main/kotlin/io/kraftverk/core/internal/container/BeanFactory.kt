package io.kraftverk.core.internal.container

import io.kraftverk.core.binding.BeanImpl
import io.kraftverk.core.common.BeanDefinition
import io.kraftverk.core.common.BeanProcessor
import io.kraftverk.core.declaration.LifecycleActions
import io.kraftverk.core.internal.binding.BeanDelegate
import io.kraftverk.core.internal.binding.BeanProviderFactory

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
