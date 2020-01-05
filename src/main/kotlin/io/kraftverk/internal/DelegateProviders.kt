/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

@PublishedApi
internal data class ValueConfig<T : Any>(
    val type: KClass<T>,
    val default: String?,
    val lazy: Boolean?,
    val secret: Boolean,
    val instance: ValueDefinition.(String) -> T
)

@PublishedApi
internal fun <T : Any> newDelegateProvider(
    name: String?,
    config: ValueConfig<T>
): DelegateProvider<Module, Value<T>> = object : DelegateProvider<Module, Value<T>> {

    override fun provideDelegate(thisRef: Module, prop: KProperty<*>): ReadOnlyProperty<Module, Value<T>> {
        val valueName = (name ?: prop.name).toValueName(thisRef.namespace)
        val valueInstance = thisRef.container.newValue(valueName, config)
        return object : ReadOnlyProperty<Module, Value<T>> {
            override fun getValue(thisRef: Module, property: KProperty<*>): Value<T> {
                return valueInstance
            }
        }
    }

    private fun String.toValueName(namespace: String) =
        (if (namespace.isBlank()) this else "${namespace}.$this").spinalCase()

}

@PublishedApi
internal data class BeanConfig<T : Any>(
    val type: KClass<T>,
    val lazy: Boolean? = null,
    val instance: BeanDefinition.() -> T
)

@PublishedApi
internal fun <T : Any> newDelegateProvider(
    config: BeanConfig<T>
): DelegateProvider<Module, Bean<T>> = object : DelegateProvider<Module, Bean<T>> {

    override fun provideDelegate(thisRef: Module, prop: KProperty<*>): ReadOnlyProperty<Module, Bean<T>> {
        val beanName = prop.name.toBeanName(thisRef.namespace)
        val beanInstance = thisRef.container.newBean(beanName, config)
        return object : ReadOnlyProperty<Module, Bean<T>> {
            override fun getValue(thisRef: Module, property: KProperty<*>): Bean<T> {
                return beanInstance
            }
        }
    }

    private fun String.toBeanName(namespace: String) =
        (if (namespace.isEmpty()) this else "${namespace}.$this")
}

internal fun <M : Module> newDelegateProvider(
    name: String? = null,
    module: () -> M
): DelegateProvider<Module, M> = object : DelegateProvider<Module, M> {

    override fun provideDelegate(thisRef: Module, prop: KProperty<*>): ReadOnlyProperty<Module, M> {
        val moduleName = name ?: prop.name
        val subModule = if (moduleName.isEmpty()) module() else {
            val currentNamespace = thisRef.namespace
            val newNamespace = if (currentNamespace.isEmpty()) moduleName else "$currentNamespace.$moduleName"
            ModuleCreationContext.use(newNamespace) {
                module()
            }
        }
        return object : ReadOnlyProperty<Module, M> {
            override fun getValue(thisRef: Module, property: KProperty<*>): M {
                return subModule
            }
        }
    }

}


