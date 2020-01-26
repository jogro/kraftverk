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
internal fun <T : Any> newValueDelegate(
    name: String?,
    config: ValueConfig<T>
): ValueDelegate<T> = object : ValueDelegate<T> {

    override fun provideDelegate(thisRef: Module, property: KProperty<*>): Delegate<Value<T>> {
        val valueName = (name ?: property.name).toQualifiedName(thisRef.namespace).spinalCase()
        val value = thisRef.container.newValue(valueName, config)
        return object : Delegate<Value<T>> {
            override fun getValue(thisRef: Module, property: KProperty<*>): Value<T> {
                return value
            }
        }
    }

}

@PublishedApi
internal data class BeanConfig<T : Any>(
    val type: KClass<T>,
    val lazy: Boolean?,
    val refreshable: Boolean?,
    val instance: BeanDefinition.() -> T
)

@PublishedApi
internal fun <T : Any> newBeanDelegate(
    config: BeanConfig<T>
): BeanDelegate<T> = object : BeanDelegate<T> {

    override fun provideDelegate(thisRef: Module, property: KProperty<*>): Delegate<Bean<T>> {
        val beanName = property.name.toQualifiedName(thisRef.namespace)
        val bean = thisRef.container.newBean(beanName, config)
        return object : Delegate<Bean<T>> {
            override fun getValue(thisRef: Module, property: KProperty<*>): Bean<T> {
                return bean
            }
        }
    }

}

internal fun <M : Module> newModuleDelegate(
    name: String? = null,
    subModule: () -> M
): ModuleDelegate<M> = object : ModuleDelegate<M> {

    override fun provideDelegate(thisRef: Module, property: KProperty<*>): Delegate<M> {
        val moduleName = (name ?: property.name).toQualifiedName(thisRef.namespace)
        val module = ModuleCreationContext.use(moduleName) { subModule() }
        return object : Delegate<M> {
            override fun getValue(thisRef: Module, property: KProperty<*>): M {
                return module
            }
        }
    }

}

private fun String.toQualifiedName(namespace: String) =
    (if (namespace.isEmpty()) this else "${namespace}.$this")
