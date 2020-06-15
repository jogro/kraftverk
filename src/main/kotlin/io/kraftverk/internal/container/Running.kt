/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.container

import io.kraftverk.binding.Bean
import io.kraftverk.binding.Binding
import io.kraftverk.binding.Component
import io.kraftverk.binding.Value
import io.kraftverk.binding.handler
import io.kraftverk.binding.provider
import io.kraftverk.declaration.ComponentDeclaration
import io.kraftverk.declaration.ValueDeclaration
import io.kraftverk.internal.binding.stop
import io.kraftverk.internal.container.Container.State
import io.kraftverk.internal.misc.mightBe
import io.kraftverk.internal.misc.mustBe
import io.kraftverk.provider.ComponentProvider
import io.kraftverk.provider.Provider
import io.kraftverk.provider.ValueProvider
import io.kraftverk.provider.instanceId

internal val Container.componentProviders: List<ComponentProvider<*, *>>
    get() =
        providers.filterIsInstance<ComponentProvider<*, *>>()

internal val Container.valueProviders: List<ValueProvider<*>>
    get() =
        providers.filterIsInstance<ValueProvider<*>>()

internal fun <T : Any> Container.createComponentInstance(
    instance: ComponentDeclaration.() -> T
): T {
    state.mustBe<State.Running>()
    return ComponentDeclaration(this).instance()
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
            return bindings.map {
                when (it) {
                    is Value<*> -> it.provider
                    is Component<*, *> -> it.provider
                    is Bean<*> -> it.provider
                }
            }
        }
    }

private fun throwValueNotFound(name: String): Nothing =
    throw ValueNotFoundException("Value '$name' was not found!", name)

private fun List<Binding<*>>.destroy() {
    filter { binding ->
        when (binding) {
            is Bean<*> -> binding.provider.instanceId != null
            is Value<*> -> binding.provider.instanceId != null
            is Component<*, *> -> binding.provider.instanceId != null
        }
    }.sortedByDescending { binding ->
        when (binding) {
            is Bean<*> -> binding.provider.instanceId
            is Value<*> -> binding.provider.instanceId
            is Component<*, *> -> binding.provider.instanceId
        }
    }.forEach { binding ->
        when (binding) {
            is Bean<*> -> binding.handler.stop()
            is Value<*> -> binding.handler.stop()
            is Component<*, *> -> binding.handler.stop()
        }
    }
}
