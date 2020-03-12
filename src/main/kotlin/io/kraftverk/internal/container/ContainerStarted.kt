package io.kraftverk.internal.container

import io.kraftverk.binding.Binding
import io.kraftverk.binding.handler
import io.kraftverk.binding.provider
import io.kraftverk.definition.BeanDefinition
import io.kraftverk.definition.ValueDefinition
import io.kraftverk.internal.binding.stop
import io.kraftverk.internal.misc.applyAs
import io.kraftverk.internal.misc.applyWhen
import io.kraftverk.internal.misc.narrow
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
    checkContainerIsRunning()
    return BeanDefinition(this).instance()
}

internal fun <T : Any> Container.createValueInstance(
    name: String,
    default: String?,
    instance: ValueDefinition.(Any) -> T
): T {
    checkContainerIsRunning()
    val definition = ValueDefinition(this)
    val value = environment[name] ?: default ?: throwValueNotFound(
        name
    )
    return definition.instance(value)
}

internal fun Container.checkContainerIsRunning() {
    state.narrow<Container.State.Started>()
}

internal fun Container.stop() =
    state.applyWhen<Container.State.Started> {
        state = Container.State.Destroying
        bindings.destroy()
        state = Container.State.Destroyed
    }

private val Container.providers: List<Provider<*>>
    get() {
        state.applyAs<Container.State.Started> {
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
