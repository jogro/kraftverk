/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.common.BeanDefinition
import io.kraftverk.declaration.BeanDeclaration
import io.kraftverk.internal.container.createBean
import io.kraftverk.internal.container.createBeanInstance
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

inline fun <reified T : Any> AbstractModule.bean(
    lazy: Boolean? = null,
    noinline instance: BeanDeclaration.() -> T
): BeanComponent<T> = bean(T::class, lazy, instance)

@PublishedApi
internal fun <T : Any> AbstractModule.bean(
    type: KClass<T>,
    lazy: Boolean? = null,
    instance: BeanDeclaration.() -> T

): BeanComponent<T> = object : BeanComponent<T> {

    override fun provideDelegate(
        thisRef: AbstractModule,
        property: KProperty<*>
    ): ReadOnlyProperty<AbstractModule, Bean<T>> {
        val beanName = qualifyName(property.name)
        logger.debug { "Creating bean '$beanName'" }
        val config = BeanDefinition(
            name = beanName,
            lazy = lazy ?: container.lazy,
            type = type,
            instance = { container.createBeanInstance(instance) }
        )
        return container.createBean(config).let(::Delegate)
    }
}
