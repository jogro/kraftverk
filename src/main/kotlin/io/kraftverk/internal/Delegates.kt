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

    override fun provideDelegate(thisRef: Module, property: KProperty<*>): Property<Value<T>> {
        val valueName = (name ?: property.name).toQualifiedName(thisRef).toSpinalCase()
        val value = thisRef.container.newValue(valueName, config)
        return object : Property<Value<T>> {
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

    override fun provideDelegate(thisRef: Module, property: KProperty<*>): Property<Bean<T>> {
        val beanName = property.name.toQualifiedName(thisRef)
        val bean = thisRef.container.newBean(beanName, config)
        return object : Property<Bean<T>> {
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

    override fun provideDelegate(thisRef: Module, property: KProperty<*>): Property<M> {
        val moduleName = (name ?: property.name).toQualifiedName(thisRef)
        val module = ModuleCreationContext.use(moduleName) { subModule() }
        return object : Property<M> {
            override fun getValue(thisRef: Module, property: KProperty<*>): M {
                return module
            }
        }
    }

}

private fun String.toQualifiedName(module: Module) =
    (if (module.namespace.isBlank()) this else "${module.namespace}.$this")
