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
    private val propertyValues: PropertyValues,
    private val defaultLazy: Boolean
) {

    private val _properties = mutableListOf<Property<*>>()

    val properties get(): List<Property<*>> = _properties

    fun <T : Any> newProperty(
        type: KClass<T>,
        name: String?,
        default: String?,
        lazy: Boolean?,
        secret: Boolean,
        namespace: String,
        instance: PropertyDefinition.(String) -> T
    ): DelegateProvider<Module, Property<T>> = object : DelegateProvider<Module, Property<T>> {
        override operator fun provideDelegate(
            thisRef: Module,
            prop: KProperty<*>
        ): ReadOnlyProperty<Module, Property<T>> {
            val definition = PropertyDefinition(propertyValues.profiles)
            val propertyName = propertyName(name ?: prop.name, namespace)
            return PropertyImpl(
                delegate = PropertyDelegate(
                    name = propertyName,
                    secret = secret,
                    type = type,
                    lazy = lazy ?: defaultLazy,
                    instance = {
                        definition.instance(getProperty(propertyName, default))
                    }
                )
            ).also {
                _properties.add(it)
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
        propertyValues[it] ?: defaultValue ?: throw PropertyNotFoundException("Property '$it' was not found!")
    }

    private fun propertyName(name: String, namespace: String) =
        (if (namespace.isEmpty()) name else "${namespace}.$name").spinalCase()

    companion object {
        private val spinalRegex = "([A-Z]+)".toRegex()

        private fun String.spinalCase(): String {
            return replace(spinalRegex, "\\-$1").toLowerCase()
        }
    }
}
