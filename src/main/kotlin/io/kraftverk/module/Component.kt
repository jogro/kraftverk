/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.Value
import io.kraftverk.definition.BeanDefinition
import io.kraftverk.definition.ValueDefinition
import io.kraftverk.internal.binding.BindingConfig
import io.kraftverk.internal.container.createBean
import io.kraftverk.internal.container.createBeanInstance
import io.kraftverk.internal.container.createValue
import io.kraftverk.internal.container.createValueInstance
import io.kraftverk.internal.module.createSubModule
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

interface Component<out T> {
    operator fun provideDelegate(thisRef: Module, property: KProperty<*>): ReadOnlyProperty<Module, T>
}

interface BeanComponent<out T : Any> : Component<Bean<T>>
interface ValueComponent<out T : Any> : Component<Value<T>>
interface SubModuleComponent<out T : Module> : Component<T>

@PublishedApi
internal fun <T : Any> Module.createBeanComponent(
    type: KClass<T>,
    lazy: Boolean?,
    instance: BeanDefinition.() -> T
): BeanComponent<T> = object : BeanComponent<T> {

    override fun provideDelegate(
        thisRef: Module,
        property: KProperty<*>
    ): ReadOnlyProperty<Module, Bean<T>> {
        val config = BindingConfig(
            name = property.name.toQualifiedName(thisRef),
            lazy = lazy ?: container.lazy,
            secret = false,
            type = type,
            instance = { container.createBeanInstance(instance) }
        )
        return container.createBean(config).let(::Delegate)
    }
}

@PublishedApi
internal fun <T : Any> Module.createValueComponent(
    name: String?,
    type: KClass<T>,
    default: String?,
    lazy: Boolean?,
    secret: Boolean,
    instance: ValueDefinition.(Any) -> T
): ValueComponent<T> = object : ValueComponent<T> {

    override fun provideDelegate(
        thisRef: Module,
        property: KProperty<*>
    ): ReadOnlyProperty<Module, Value<T>> {
        val valueName = (name ?: property.name).toQualifiedName(thisRef).toSpinalCase()
        val config = BindingConfig(
            name = valueName,
            lazy = lazy ?: container.lazy,
            secret = secret,
            type = type,
            instance = { container.createValueInstance(valueName, default, instance) }
        )
        return container.createValue(config).let(::Delegate)
    }
}

internal fun <M : Module> createSubModuleComponent(
    name: String? = null,
    instance: () -> M
): SubModuleComponent<M> = object :
    SubModuleComponent<M> {

    override fun provideDelegate(
        thisRef: Module,
        property: KProperty<*>
    ): ReadOnlyProperty<Module, M> {
        val moduleName = (name ?: property.name).toQualifiedName(thisRef)
        val module = createSubModule(moduleName, instance)
        return Delegate(module)
    }
}

private class Delegate<T : Any>(private val t: T) :
    ReadOnlyProperty<Module, T> {
    override fun getValue(thisRef: Module, property: KProperty<*>): T {
        return t
    }
}

private fun String.toQualifiedName(module: Module) =
    (if (module.namespace.isBlank()) this else "${module.namespace}.$this")

private val spinalRegex = "([A-Z]+)".toRegex()

private fun String.toSpinalCase() = replace(spinalRegex, "\\-$1").toLowerCase()
