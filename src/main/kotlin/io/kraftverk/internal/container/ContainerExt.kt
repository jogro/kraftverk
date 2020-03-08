package io.kraftverk.internal.container

import io.kraftverk.binding.BeanImpl
import io.kraftverk.binding.ValueImpl
import io.kraftverk.definition.BeanDefinition
import io.kraftverk.definition.ValueDefinition
import io.kraftverk.internal.binding.BeanHandler
import io.kraftverk.internal.binding.ValueHandler
import io.kraftverk.module.BeanConfig
import io.kraftverk.module.ValueConfig

internal fun <T : Any> Container.newBean(
    name: String,
    config: BeanConfig<T>
) = createBeanHandler(name, config).let(::BeanImpl).apply(::register)

internal fun <T : Any> Container.newValue(
    name: String,
    config: ValueConfig<T>
) = createValueHandler(name, config).let(::ValueImpl).apply(::register)

private fun <T : Any> Container.createBeanHandler(
    name: String,
    config: BeanConfig<T>
) = BeanHandler(
    name = name,
    type = config.type,
    lazy = config.lazy ?: lazy,
    instanceFactory = {
        checkIsStarted()
        config.createInstance(BeanDefinition(environment))
    }
)

private fun <T : Any> Container.createValueHandler(
    name: String,
    config: ValueConfig<T>
) = ValueHandler(
    name = name,
    secret = config.secret,
    type = config.type,
    lazy = config.lazy ?: lazy,
    instanceFactory = {
        checkIsStarted()
        config.createInstance(
            ValueDefinition(environment),
            environment[name] ?: config.default
            ?: throwValueNotFound(name)
        )
    }
)

private fun throwValueNotFound(name: String): Nothing =
    throw IllegalStateException("Value '$name' was not found!")
