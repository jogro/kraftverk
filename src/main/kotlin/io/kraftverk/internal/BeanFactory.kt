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
    private val moduleContext: AppContext
) {

    fun <T : Any> newPrototype(
        type: KClass<T>,
        createInstance: PrototypeDefinition.() -> T
    ): DelegateProvider<Module, PrototypeBean<T>> = object :
        DelegateProvider<Module, PrototypeBean<T>> {
        override fun provideDelegate(thisRef: Module, prop: KProperty<*>): ReadOnlyProperty<Module, PrototypeBean<T>> {
            val definition = PrototypeDefinition(moduleContext)
            return PrototypeBeanImpl(
                binding = PrototypeBinding(
                    type = type,
                    initialState = DefiningBinding(
                        lazy = true,
                        supply = {
                            definition.createInstance()
                        }
                    )
                )
            ).also {
                moduleContext.registerBean(it)
            }.let {
                object : ReadOnlyProperty<Module, PrototypeBean<T>> {
                    override fun getValue(thisRef: Module, property: KProperty<*>): PrototypeBean<T> {
                        return it
                    }
                }
            }
        }
    }


    fun <T : Any> newSingleton(
        type: KClass<T>,
        lazy: Boolean? = null,
        createInstance: SingletonDefinition.() -> T
    ): DelegateProvider<Module, SingletonBean<T>> = object :
        DelegateProvider<Module, SingletonBean<T>> {
        override fun provideDelegate(
            thisRef: Module,
            prop: KProperty<*>
        ): ReadOnlyProperty<Module, SingletonBean<T>> {
            val definition = SingletonDefinition(moduleContext)
            return SingletonBeanImpl(
                binding = SingletonBinding(
                    type = type,
                    initialState = DefiningBinding(
                        lazy = lazy ?: moduleContext.defaultLazyBeans,
                        supply = {
                            definition.createInstance()
                        }
                    )
                )
            ).also {
                moduleContext.registerBean(it)
            }.let {
                object : ReadOnlyProperty<Module, SingletonBean<T>> {
                    override fun getValue(thisRef: Module, property: KProperty<*>): SingletonBean<T> {
                        return it
                    }
                }
            }
        }
    }

}


