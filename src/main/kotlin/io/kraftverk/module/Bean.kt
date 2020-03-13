package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.definition.BeanDefinition
import io.kraftverk.internal.binding.BeanConfig
import io.kraftverk.internal.container.createBean
import io.kraftverk.internal.container.createBeanInstance
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

inline fun <reified T : Any> Module.bean(
    lazy: Boolean? = null,
    noinline instance: BeanDefinition.() -> T
): BeanComponent<T> = createBeanComponent(
    T::class,
    lazy,
    instance
)

@PublishedApi
internal fun <T : Any> Module.createBeanComponent(
    type: KClass<T>,
    lazy: Boolean?,
    instance: BeanDefinition.() -> T
): BeanComponent<T> = object :
    BeanComponent<T> {

    override fun provideDelegate(
        thisRef: Module,
        property: KProperty<*>
    ): ReadOnlyProperty<Module, Bean<T>> {
        val beanName = qualifyName(property.name)
        logger.debug { "Creating bean '$beanName'" }
        val config = BeanConfig(
            name = beanName,
            lazy = lazy ?: container.lazy,
            type = type,
            instance = { container.createBeanInstance(instance) }
        )
        return container.createBean(config).let(::Delegate)
    }
}
