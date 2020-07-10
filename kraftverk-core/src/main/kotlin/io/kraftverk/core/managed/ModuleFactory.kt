package io.kraftverk.core.managed

import io.kraftverk.core.common.BeanProcessor
import io.kraftverk.core.common.ValueProcessor
import io.kraftverk.core.env.Environment
import io.kraftverk.core.internal.container.Container
import io.kraftverk.core.internal.container.start
import io.kraftverk.core.internal.misc.Consumer
import io.kraftverk.core.internal.misc.interceptAfter
import io.kraftverk.core.module.Module

internal class ModuleFactory<M : Module>(
    private val env: Environment,
    private val namespace: String,
    private val instance: () -> M
) {
    private val beanProcessors = mutableListOf<BeanProcessor>()
    private val valueProcessors = mutableListOf<ValueProcessor>()
    private var onConfigure: Consumer<M> = {}

    fun addProcessor(processor: BeanProcessor) {
        beanProcessors += processor
    }

    fun addProcessor(processor: ValueProcessor) {
        valueProcessors += processor
    }

    fun configure(block: M.() -> Unit) {
        onConfigure = interceptAfter(onConfigure, block)
    }

    fun createModule(lazy: Boolean): M {
        val container =
            Container(env, beanProcessors, valueProcessors)
        val module = io.kraftverk.core.internal.module.createModule(container, namespace, instance)
        onConfigure(module)
        container.start(lazy)
        return module
    }
}
