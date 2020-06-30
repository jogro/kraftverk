package io.kraftverk.managed

import io.kraftverk.common.BeanProcessor
import io.kraftverk.common.ValueProcessor
import io.kraftverk.env.Environment
import io.kraftverk.internal.container.Container
import io.kraftverk.internal.container.start
import io.kraftverk.internal.misc.Consumer
import io.kraftverk.internal.misc.interceptAfter
import io.kraftverk.module.Module

class ModuleFactory<M : Module>(
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
        val container = Container(env, beanProcessors, valueProcessors)
        val module = io.kraftverk.module.createModule(container, namespace, instance)
        onConfigure(module)
        container.start(lazy)
        return module
    }
}
