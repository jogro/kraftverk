/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.Value
import io.kraftverk.definition.BeanDefinition
import io.kraftverk.definition.ValueDefinition
import io.kraftverk.internal.container.createBean
import io.kraftverk.internal.container.createValue
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
internal data class BeanConfig<T : Any>(
    val type: KClass<T>,
    val lazy: Boolean?,
    val createInstance: BeanDefinition.() -> T
)

@PublishedApi
internal data class ValueConfig<T : Any>(
    val type: KClass<T>,
    val default: String?,
    val lazy: Boolean?,
    val secret: Boolean,
    val createInstance: ValueDefinition.(Any) -> T
)

@PublishedApi
internal fun <T : Any> createBeanComponent(
    config: BeanConfig<T>
): BeanComponent<T> = object :
    BeanComponent<T> {

    override fun provideDelegate(
        thisRef: Module,
        property: KProperty<*>
    ): ReadOnlyProperty<Module, Bean<T>> {
        val beanName = property.name.toQualifiedName(thisRef)
        val bean = thisRef.container.createBean(beanName, config)
        return Delegate(bean)
    }
}

@PublishedApi
internal fun <T : Any> createValueComponent(
    name: String?,
    config: ValueConfig<T>
): ValueComponent<T> = object :
    ValueComponent<T> {

    override fun provideDelegate(
        thisRef: Module,
        property: KProperty<*>
    ): ReadOnlyProperty<Module, Value<T>> {
        val valueName = (name ?: property.name).toQualifiedName(thisRef).toSpinalCase()
        val value = thisRef.container.createValue(valueName, config)
        return Delegate(value)
    }
}

internal fun <M : Module> createSubModuleComponent(
    name: String? = null,
    subModule: () -> M
): SubModuleComponent<M> = object :
    SubModuleComponent<M> {

    override fun provideDelegate(
        thisRef: Module,
        property: KProperty<*>
    ): ReadOnlyProperty<Module, M> {
        val moduleName = (name ?: property.name).toQualifiedName(thisRef)
        val module = createSubModule(moduleName) { subModule() }
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
