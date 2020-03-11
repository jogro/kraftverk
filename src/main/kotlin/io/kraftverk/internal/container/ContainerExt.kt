package io.kraftverk.internal.container

import io.kraftverk.binding.BeanImpl
import io.kraftverk.binding.ValueImpl
import io.kraftverk.definition.BeanDefinition
import io.kraftverk.definition.ValueDefinition
import io.kraftverk.internal.binding.BeanHandler
import io.kraftverk.internal.binding.BindingConfig
import io.kraftverk.internal.binding.ValueHandler
import io.kraftverk.provider.BeanProvider
import io.kraftverk.provider.ValueProvider

internal val Container.beanProviders: List<BeanProvider<*>>
    get() =
        providers.filterIsInstance<BeanProvider<*>>()

internal val Container.valueProviders: List<ValueProvider<*>>
    get() =
        providers.filterIsInstance<ValueProvider<*>>()

internal fun <T : Any> Container.createBean(config: BindingConfig<T>): BeanImpl<T> =
    BeanHandler(config)
        .let(::BeanImpl)
        .also(this::register)

internal fun <T : Any> Container.createBeanInstance(
    instance: BeanDefinition.() -> T
): T {
    checkContainerIsRunning()
    return BeanDefinition(this).instance()
}

internal fun <T : Any> Container.createValue(
    config: BindingConfig<T>
): ValueImpl<T> = ValueHandler(config)
    .let(::ValueImpl)
    .also(this::register)

internal fun <T : Any> Container.createValueInstance(
    name: String,
    default: String?,
    instance: ValueDefinition.(Any) -> T
): T {
    checkContainerIsRunning()
    val definition = ValueDefinition(this)
    val value = environment[name] ?: default ?: throwValueNotFound(name)
    return definition.instance(value)
}

private fun throwValueNotFound(name: String): Nothing =
    throw IllegalStateException("Value '$name' was not found!")
