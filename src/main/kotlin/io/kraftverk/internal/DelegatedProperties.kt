/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.*
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
internal fun <T : Any> newDelegate(
    name: String?,
    config: ValueConfig<T>
): DelegatedValue<T> = object : DelegatedValue<T> {

    override fun provideDelegate(thisRef: Module, property: KProperty<*>): ModuleProperty<Value<T>> {
        val valueName = (name ?: property.name).toValueName(thisRef.namespace)
        val value = thisRef.container.newValue(valueName, config)
        return object : ModuleProperty<Value<T>> {
            override fun getValue(thisRef: Module, property: KProperty<*>): Value<T> {
                return value
            }
        }
    }

    private fun String.toValueName(namespace: String) =
        (if (namespace.isBlank()) this else "${namespace}.$this").spinalCase()

}

@PublishedApi
internal data class BeanConfig<T : Any>(
    val type: KClass<T>,
    val lazy: Boolean?,
    val refreshable: Boolean?,
    val instance: BeanDefinition.() -> T
)

@PublishedApi
internal fun <T : Any> newDelegate(
    config: BeanConfig<T>
): DelegatedBean<T> = object : DelegatedBean<T> {

    override fun provideDelegate(thisRef: Module, property: KProperty<*>): ModuleProperty<Bean<T>> {
        val beanName = property.name.toBeanName(thisRef.namespace)
        val bean = thisRef.container.newBean(beanName, config)
        return object : ModuleProperty<Bean<T>> {
            override fun getValue(thisRef: Module, property: KProperty<*>): Bean<T> {
                return bean
            }
        }
    }

    private fun String.toBeanName(namespace: String) =
        (if (namespace.isEmpty()) this else "${namespace}.$this")
}

internal fun <M : Module> newDelegate(
    name: String? = null,
    subModule: () -> M
): DelegatedModule<M> = object : DelegatedModule<M> {

    override fun provideDelegate(thisRef: Module, property: KProperty<*>): ModuleProperty<M> {
        val moduleName = name ?: property.name
        val module = if (moduleName.isEmpty()) subModule() else {
            val currentNamespace = thisRef.namespace
            val newNamespace = if (currentNamespace.isEmpty()) moduleName else "$currentNamespace.$moduleName"
            ModuleCreationContext.use(newNamespace) {
                subModule()
            }
        }
        return object : ModuleProperty<M> {
            override fun getValue(thisRef: Module, property: KProperty<*>): M {
                return module
            }
        }
    }

}


