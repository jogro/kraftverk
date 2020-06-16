/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.BeanImpl
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
): BeanDelegateProvider<T, T> = bean(T::class, lazy, { i, shape -> shape(i) }, instance)

interface BeanSpi<S> {
    fun onShape(shape: (S) -> Unit)
}

inline fun <reified T : BeanSpi<S>, S : Any> AbstractModule.beanSpi(
    lazy: Boolean? = null,
    noinline instance: BeanDeclaration.() -> T
) =
    bean(T::class, lazy, { t: T, shape: (S) -> Unit -> t.onShape(shape) }, instance)

@PublishedApi
internal fun <T : Any, S : Any> AbstractModule.bean(
    type: KClass<T>,
    lazy: Boolean? = null,
    onShape: (T, (S) -> Unit) -> Unit,
    instance: BeanDeclaration.() -> T

): BeanDelegateProvider<T, S> = object : BeanDelegateProvider<T, S> {

    override fun provideDelegate(
        thisRef: AbstractModule,
        property: KProperty<*>
    ): ReadOnlyProperty<AbstractModule, Bean<T, S>> {
        val beanName = qualifyName(property.name)
        return createBean(beanName, type, lazy, onShape, instance).let(::Delegate)
    }
}

private fun <T : Any, S : Any> AbstractModule.createBean(
    propertyName: String,
    type: KClass<T>,
    lazy: Boolean? = null,
    onShape: (T, (S) -> Unit) -> Unit,
    instance: BeanDeclaration.() -> T
): BeanImpl<T, S> {
    val beanName = qualifyName(propertyName)
    val config = BeanDefinition(
        name = beanName,
        lazy = lazy ?: container.lazy,
        onShape = onShape,
        type = type,
        instance = { container.createBeanInstance(instance) }
    )
    return container.createBean(config)
}
