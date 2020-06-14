/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Component
import io.kraftverk.common.ComponentDefinition
import io.kraftverk.declaration.ComponentDeclaration
import io.kraftverk.internal.container.createComponent
import io.kraftverk.internal.container.createBeanInstance
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

inline fun <reified T : Any, S : Any> AbstractModule.component(
    lazy: Boolean? = null,
    noinline onShape: (T, (S) -> Unit) -> Unit,
    noinline instance: ComponentDeclaration.() -> T
): ComponentDelegateProvider<T, S> = component(T::class, lazy, onShape, instance)

@PublishedApi
internal fun <T : Any, S : Any> AbstractModule.component(
    type: KClass<T>,
    lazy: Boolean? = null,
    onShape: (T, (S) -> Unit) -> Unit,
    instance: ComponentDeclaration.() -> T

): ComponentDelegateProvider<T, S> = object : ComponentDelegateProvider<T, S> {

    override fun provideDelegate(
        thisRef: AbstractModule,
        property: KProperty<*>
    ): ReadOnlyProperty<AbstractModule, Component<T, S>> {
        val beanName = qualifyName(property.name)
        logger.debug { "Creating bean '$beanName'" }
        val config = ComponentDefinition(
            name = beanName,
            lazy = lazy ?: container.lazy,
            onShape = onShape,
            type = type,
            instance = { container.createBeanInstance(instance) }
        )
        return container.createComponent<T, S>(config).let(::Delegate)
    }
}
