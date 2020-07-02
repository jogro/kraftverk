/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.container

import io.kraftverk.binding.Binding
import io.kraftverk.binding.delegate
import io.kraftverk.declaration.BeanDeclaration
import io.kraftverk.declaration.LifecycleActions
import io.kraftverk.declaration.ValueDeclaration
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
    lifecycleActions: LifecycleActions,
    instance: BeanDeclaration.() -> T
): T {
    state.mustBe<State.Running>()
    return BeanDeclaration(lifecycleActions, this).instance()
}

internal fun <T : Any> Container.createValueInstance(
    name: String,
    default: T?,
    instance: ValueDeclaration.(Any) -> T
): T {
    state.mustBe<State.Running>()
    val definition = ValueDeclaration(this)
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
            return bindings.map { binding -> binding.delegate.provider }
        }
    }

private fun throwValueNotFound(name: String): Nothing =
    throw ValueNotFoundException("Value '$name' was not found!", name)

private fun List<Binding<*>>.destroy() {
    filter { binding -> binding.delegate.provider.instanceId != null }
        .sortedByDescending { binding -> binding.delegate.provider.instanceId }
        .forEach { binding -> binding.delegate.stop() }
}
