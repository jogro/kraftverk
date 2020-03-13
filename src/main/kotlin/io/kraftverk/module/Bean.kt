package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.definition.BeanDefinition
import io.kraftverk.internal.binding.BeanConfig
import io.kraftverk.internal.container.createBean
import io.kraftverk.internal.container.createBeanInstance
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// TODO Make this configurable
private val beanParamsTransformer = defaultBeanParamsTransformer()

inline fun <reified T : Any> Module.bean(
    lazy: Boolean? = null,
    noinline instance: BeanDefinition.() -> T
): BeanComponent<T> = BeanParams(
    T::class,
    lazy,
    instance
).let(::createBeanComponent)

@PublishedApi
internal fun <T : Any> Module.createBeanComponent(
    params: BeanParams<T>
): BeanComponent<T> = object : BeanComponent<T> {

    override fun provideDelegate(
        thisRef: Module,
        property: KProperty<*>
    ): ReadOnlyProperty<Module, Bean<T>> {

        val transformed = beanParamsTransformer.transform(namespace, property.name, params)

        val beanName = qualifyName(property.name)
        logger.debug { "Creating bean '$beanName'" }
        val config = BeanConfig(
            name = beanName,
            lazy = transformed.lazy ?: container.lazy,
            type = transformed.type,
            instance = { container.createBeanInstance(transformed.instance) }
        )
        return container.createBean(config).let(::Delegate)
    }
}
