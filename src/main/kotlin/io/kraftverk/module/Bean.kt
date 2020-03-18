/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.common.BeanConfig
import io.kraftverk.definition.BeanDefinition
import io.kraftverk.internal.container.createBean
import io.kraftverk.internal.container.createBeanInstance
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

inline fun <reified T : Any> Modular.bean(
    lazy: Boolean? = null,
    noinline instance: BeanDefinition.() -> T
): BeanComponent<T> = bean(T::class, lazy, instance)

fun <T : Any> Modular.bean(
    type: KClass<T>,
    lazy: Boolean? = null,
    instance: BeanDefinition.() -> T

): BeanComponent<T> = object : BeanComponent<T> {

    override fun provideDelegate(
        thisRef: Modular,
        property: KProperty<*>
    ): ReadOnlyProperty<Modular, Bean<T>> {
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
