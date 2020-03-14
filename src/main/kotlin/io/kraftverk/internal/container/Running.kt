/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.container

import io.kraftverk.binding.Binding
import io.kraftverk.binding.handler
import io.kraftverk.binding.provider
import io.kraftverk.definition.BeanDefinition
import io.kraftverk.definition.ValueDefinition
import io.kraftverk.internal.binding.stop
import io.kraftverk.internal.container.Container.State
import io.kraftverk.internal.misc.mightBe
import io.kraftverk.internal.misc.mustBe
import io.kraftverk.provider.BeanProvider
import io.kraftverk.provider.Provider
import io.kraftverk.provider.ValueProvider
import io.kraftverk.provider.instanceId

internal val Container.beanProviders: List<BeanProvider<*>>
    get() =
        providers.filterIsInstance<BeanProvider<*>>()

internal val Container.valueProviders: List<ValueProvider<*>>
    get() =
        providers.filterIsInstance<ValueProvider<*>>()

internal fun <T : Any> Container.createBeanInstance(
    instance: BeanDefinition.() -> T
): T {
    state.mustBe<State.Running>()
    return BeanDefinition(this).instance()
}

internal fun <T : Any> Container.createValueInstance(
    name: String,
    default: T?,
    instance: ValueDefinition.(Any) -> T
): T {
    state.mustBe<State.Running>()
    val definition = ValueDefinition(this)
    val value = environment[name] ?: default ?: throwValueNotFound(
        name
    )
    return definition.instance(value)
}

internal fun Container.stop() =
    state.mightBe<State.Running> {
        state = State.Destroying
        bindings.destroy()
        state = State.Destroyed
    }

private val Container.providers: List<Provider<*>>
    get() {
        state.mustBe<State.Running> {
            return bindings.map { it.provider }
        }
    }

private fun throwValueNotFound(name: String): Nothing =
    throw IllegalStateException("Value '$name' was not found!")

private fun List<Binding<*>>.destroy() {
    filter { binding ->
        binding.provider.instanceId != null
    }.sortedByDescending { binding ->
        binding.provider.instanceId
    }.forEach { binding ->
        binding.handler.stop()
    }
}
