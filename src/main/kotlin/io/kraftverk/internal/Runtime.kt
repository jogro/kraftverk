/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.Bean
import io.kraftverk.Property

const val ACTIVE_PROFILES = "kraftverk.active.profiles"

internal class Runtime(
    internal val lazyBeans: Boolean,
    internal val lazyProps: Boolean,
    private val propertyReader: (List<String>) -> (String) -> String?
) {

    private var state: RuntimeState = RuntimeConfiguration()

    val profiles: List<String> by lazy { profiles() }

    fun setProperty(name: String, value: String) {
        state.runAs<RuntimeConfiguration> {
            customizedPropertyValues[name] = value
        }
    }

    fun registerBean(bean: Bean<*>) {
        state.runAs<RuntimeConfiguration> {
            beans.add(bean)
        }
    }

    fun registerProperty(property: Property<*>) {
        state.runAs<RuntimeConfiguration> {
            properties.add(property)
        }
    }

    fun prepare() {
        state.runAs<RuntimeConfiguration> {
            properties.forEach { it.prepare() }
            beans.forEach { it.prepare() }
            state = PreparedRuntime(
                newPropertyValueResolver(customizedPropertyValues, propertyReader),
                properties,
                beans
            )
        }
    }

    fun start() {
        state.runAs<PreparedRuntime> {
            properties.forEach { it.evaluate() }
            beans.forEach { it.evaluate() }
        }
    }

    operator fun get(name: String): String? {
        state.runAs<PreparedRuntime> {
            return propertyValueResolver[name]
        }
    }

    private fun profiles(): List<String> {
        state.runAs<PreparedRuntime> {
            return propertyValueResolver.profiles
        }
    }

    fun destroy() {
        state.runIf<PreparedRuntime> {
            beans.filter { it.provider().instanceId != null }
                .sortedByDescending { it.provider().instanceId }
                .forEach { bean ->
                    runCatching {
                        bean.destroy()
                    }.onFailure { ex ->
                        ex.printStackTrace()
                    }
                }
            state = DestroyedRuntime
        }

    }

}
