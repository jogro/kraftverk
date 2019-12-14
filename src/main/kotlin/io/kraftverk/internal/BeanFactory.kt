/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

internal class BeanFactory(
    private val registry: Registry,
    private val namespace: String
) {

    fun <T : Any> newBean(
        type: KClass<T>,
        lazy: Boolean? = null,
        instance: BeanDefinition.() -> T
    ): DelegateProvider<Module, Bean<T>> = object : DelegateProvider<Module, Bean<T>> {
        override fun provideDelegate(
            thisRef: Module,
            prop: KProperty<*>
        ): ReadOnlyProperty<Module, Bean<T>> {
            val definition = BeanDefinition(registry)
            return BeanImpl(
                delegate = BeanDelegate(
                    name = beanName(prop.name),
                    type = type,
                    lazy = lazy ?: registry.lazyBeans,
                    instance = {
                        definition.instance()
                    }
                )
            ).also {
                registry.registerBean(it)
            }.let {
                object : ReadOnlyProperty<Module, Bean<T>> {
                    override fun getValue(thisRef: Module, property: KProperty<*>): Bean<T> {
                        return it
                    }
                }
            }
        }
    }

    private fun beanName(name: String) =
        (if (namespace.isEmpty()) name else "${namespace}.$name")

}


