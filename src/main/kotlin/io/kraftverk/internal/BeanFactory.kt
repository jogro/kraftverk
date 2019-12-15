/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

internal class BeanFactory(val profiles: List<String>, val defaultLazy: Boolean) {

    private val _beans = mutableListOf<Bean<*>>()

    val beans get(): List<Bean<*>> = _beans

    fun <T : Any> newBean(
        type: KClass<T>,
        lazy: Boolean? = null,
        namespace: String,
        instance: BeanDefinition.() -> T
    ): DelegateProvider<Module, Bean<T>> = object : DelegateProvider<Module, Bean<T>> {
        override fun provideDelegate(
            thisRef: Module,
            prop: KProperty<*>
        ): ReadOnlyProperty<Module, Bean<T>> {
            val definition = BeanDefinition(profiles)
            return BeanImpl(
                delegate = BeanDelegate(
                    name = beanName(prop.name, namespace),
                    type = type,
                    lazy = lazy ?: defaultLazy,
                    instance = {
                        definition.instance()
                    }
                )
            ).also {
                _beans.add(it)
            }.let {
                object : ReadOnlyProperty<Module, Bean<T>> {
                    override fun getValue(thisRef: Module, property: KProperty<*>): Bean<T> {
                        return it
                    }
                }
            }
        }
    }

    private fun beanName(name: String, namespace: String) =
        (if (namespace.isEmpty()) name else "${namespace}.$name")

}


