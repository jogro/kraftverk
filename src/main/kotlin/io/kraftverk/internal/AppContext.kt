/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.Bean
import io.kraftverk.Property

const val ACTIVE_PROFILES = "kraftverk.active.profiles"

internal class AppContext(
    internal val defaultLazyBeans: Boolean,
    internal val defaultLazyProps: Boolean,
    private val propertyReader: (List<String>) -> (String) -> String?
) {

    private var state: AppContextState = DefiningAppContext()

    val profiles: List<String> by lazy { profiles() }

    fun setProperty(name: String, value: String) {
        state.expect<DefiningAppContext> {
            customizedPropertyValues[name] = value
        }
    }

    fun registerBean(bean: Bean<*>) {
        state.expect<DefiningAppContext> {
            beans.add(bean)
        }
    }

    fun registerProperty(property: Property) {
        state.expect<DefiningAppContext> {
            properties.add(property)
        }
    }

    fun initialize() {
        state.expect<DefiningAppContext> {
            properties.forEach { it.initialize() }
            beans.forEach { it.initialize() }
            state = InitializedAppContext(
                newPropertyValueResolver(customizedPropertyValues, propertyReader),
                properties,
                beans
            )
        }
    }

    fun start() {
        state.expect<InitializedAppContext> {
            properties.forEach { it.start() }
            beans.forEach { it.start() }
        }
    }

    operator fun get(name: String): String? {
        state.expect<InitializedAppContext> {
            return propertyValueResolver[name]
        }
    }

    private fun profiles(): List<String> {
        state.expect<InitializedAppContext> {
            return propertyValueResolver.profiles
        }
    }

    fun destroy() {
        state.on<InitializedAppContext> {
            beans.filter { it.instanceId() != null }
                .sortedByDescending { it.instanceId() }
                .forEach { bean ->
                    runCatching {
                        bean.destroy()
                    }.onFailure { ex ->
                        ex.printStackTrace()
                    }
                }
            state = DestroyedAppContext
        }

    }

}
