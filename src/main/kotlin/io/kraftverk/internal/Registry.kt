/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.Bean
import io.kraftverk.Property
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

const val ACTIVE_PROFILES = "kraftverk.active.profiles"

internal class Registry(
    internal val lazyBeans: Boolean,
    internal val lazyProps: Boolean,
    private val propertyReader: (List<String>) -> (String) -> String?
) {

    private var state: RegistryState = RegistryState.Configuring()

    val profiles: List<String> by lazy { profiles() }

    fun registerBean(bean: Bean<*>) {
        state.runAs<RegistryState.Configuring> {
            beans.add(bean)
        }
    }

    fun registerProperty(property: Property<*>) {
        state.runAs<RegistryState.Configuring> {
            properties.add(property)
        }
    }

    fun start() {
        state.runAs<RegistryState.Configuring> {
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
        state.runAs<RegistryState.Running> {
            return propertyValues[name]
        }
    }

    private fun profiles(): List<String> {
        state.runAs<RegistryState.Running> {
            return propertyValues.profiles
        }
    }

    fun destroy() {
        state.runIf<RegistryState.Running> {
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

private inline fun <reified T : RegistryState> RegistryState.runIf(block: T.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (this is T) {
        this.block()
    }
}

private inline fun <reified T : RegistryState> RegistryState.runAs(block: T.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    if (this is T) {
        this.block()
    } else {
        throw IllegalStateException("Expected state to be ${T::class} but was ${this::class}")
    }
}
