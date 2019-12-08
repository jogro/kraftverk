/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

internal class PropertyFactory(
    private val appContext: AppContext,
    private val namespace: String
) {

    fun <T : Any> newProperty(
        type: KClass<T>,
        name: String?,
        defaultValue: String?,
        lazy: Boolean?,
        secret: Boolean,
        instance: PropertyDefinition.(String) -> T
    ): DelegateProvider<Module, Property<T>> = object : DelegateProvider<Module, Property<T>> {
        override operator fun provideDelegate(
            thisRef: Module,
            prop: KProperty<*>
        ): ReadOnlyProperty<Module, Property<T>> {
            val definition = PropertyDefinition(appContext)
            val propertyName = propertyName(name ?: prop.name)
            return PropertyImpl(
                binding = PropertyBinding(
                    name = propertyName,
                    secret = secret,
                    type = type,
                    initialState = BindingConfiguration(
                        lazy = lazy ?: appContext.lazyProps,
                        instance = {
                            definition.instance(getProperty(propertyName, defaultValue))
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

    private fun getProperty(name: String, defaultValue: String?) = name.let {
        appContext[it] ?: defaultValue ?: throw PropertyNotFoundException("Property '$it' was not found!")
    }

    private fun propertyName(name: String) =
        (if (namespace.isEmpty()) name else "${namespace}.$name").spinalCase()

    companion object {
        private val spinalRegex = "([A-Z]+)".toRegex()

        private fun String.spinalCase(): String {
            return replace(spinalRegex, "\\-$1").toLowerCase()
        }
    }
}
