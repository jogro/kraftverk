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
import kotlin.reflect.KProperty

internal class PropertyFactory(
    private val appContext: AppContext,
    private val getProperty: (String, String?) -> String
) {

    fun newProperty(
        name: String? = null,
        defaultValue: String?,
        lazy: Boolean? = null
    ): DelegateProvider<Module, Property> = object :
        DelegateProvider<Module, Property> {
        override operator fun provideDelegate(
            thisRef: Module,
            prop: KProperty<*>
        ): ReadOnlyProperty<Module, Property> = PropertyImpl(
            binding = PropertyBinding(
                initialState = DefiningBinding(
                    lazy = lazy ?: appContext.defaultLazyProps,
                    supply = {
                        getProperty((name ?: prop.name).toLowerCase(), defaultValue)
                    }
                )
            )
        ).also {
            appContext.registerProperty(it)
        }.let {
            object : ReadOnlyProperty<Module, Property> {
                override fun getValue(thisRef: Module, property: KProperty<*>): Property {
                    return it
                }
            }
        }
    }
}








