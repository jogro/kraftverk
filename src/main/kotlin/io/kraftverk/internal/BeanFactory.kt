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

    fun <T : Any> newBean(
        type: KClass<T>,
        lazy: Boolean? = null,
        createInstance: BeanDefinition.() -> T
    ): DelegateProvider<Module, Bean<T>> = object :
        DelegateProvider<Module, Bean<T>> {
        override fun provideDelegate(
            thisRef: Module,
            prop: KProperty<*>
        ): ReadOnlyProperty<Module, Bean<T>> {
            val definition = BeanDefinition(moduleContext)
            return BeanImpl(
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
                object : ReadOnlyProperty<Module, Bean<T>> {
                    override fun getValue(thisRef: Module, property: KProperty<*>): Bean<T> {
                        return it
                    }
                }
            }
        }
    }

}


