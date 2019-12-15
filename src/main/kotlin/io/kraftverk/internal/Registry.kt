/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.Bean
import io.kraftverk.Property

const val ACTIVE_PROFILES = "kraftverk.active.profiles"

internal class Registry(
    internal val lazyBeans: Boolean,
    internal val lazyProps: Boolean,
    private val propertyReader: (List<String>) -> (String) -> String?
) {

    private var state: RegistryState = RegistryState.Configuring()

    val profiles: List<String> by lazy { profiles() }

    fun registerBean(bean: Bean<*>) {
        state.applyAs<RegistryState.Configuring> {
            beans.add(bean)
        }
    }

    fun registerProperty(property: Property<*>) {
        state.applyAs<RegistryState.Configuring> {
            properties.add(property)
        }
    }

    fun start() {
        state.applyAs<RegistryState.Configuring> {
            state = RegistryState.Running(
                newPropertyValues(propertyReader),
                properties.toList(),
                beans.toList()
            )
            properties.forEach { it.start() }
            beans.forEach { it.start() }
        }
    }

    operator fun get(name: String): String? {
        state.applyAs<RegistryState.Running> {
            return propertyValues[name]
        }
    }

    private fun profiles(): List<String> {
        state.applyAs<RegistryState.Running> {
            return propertyValues.profiles
        }
    }

    fun destroy() {
        state.applyWhen<RegistryState.Running> {
            beans.filter { it.provider().instanceId != null }
                .sortedByDescending { it.provider().instanceId }
                .forEach { bean ->
                    runCatching {
                        bean.destroy()
                    }.onFailure { ex ->
                        ex.printStackTrace()
                    }
                }
            state = RegistryState.Destroyed
        }

    }

}

private sealed class RegistryState {

    class Configuring : RegistryState() {
        val properties = mutableListOf<Property<*>>()
        val beans = mutableListOf<Bean<*>>()
    }

    class Running(
        val propertyValues: PropertyValues,
        val properties: List<Property<*>>,
        val beans: List<Bean<*>>
    ) : RegistryState()

    object Destroyed : RegistryState()
}
