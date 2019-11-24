/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.DelegateProvider
import io.kraftverk.Module
import io.kraftverk.Property
import io.kraftverk.PropertyImpl
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

internal class PropertyFactory(
    private val appContext: AppContext,
    private val getProperty: (String, String?) -> String
) {

    fun <T : Any> newProperty(
        type: KClass<T>,
        name: String? = null,
        defaultValue: String?,
        lazy: Boolean? = null,
        convert: (String) -> T
    ): DelegateProvider<Module, Property<T>> = object :
        DelegateProvider<Module, Property<T>> {
        override operator fun provideDelegate(
            thisRef: Module,
            prop: KProperty<*>
        ): ReadOnlyProperty<Module, Property<T>> = PropertyImpl(
            binding = SingletonBinding(
                type = type,
                initialState = DefiningBinding(
                    lazy = lazy ?: appContext.defaultLazyProps,
                    supply = {
                        convert(getProperty((name ?: prop.name).toLowerCase(), defaultValue))
                    }
                )
            )
        ).also {
            appContext.registerProperty(it)
        }.let {
            object : ReadOnlyProperty<Module, Property<T>> {
                override fun getValue(thisRef: Module, property: KProperty<*>): Property<T> {
                    return it
                }
            }
        }
    }
}








