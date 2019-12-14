/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.PropertyNameException
import mu.KotlinLogging

private val logger = KotlinLogging.logger {  }

internal class PropertyValues(
    val profiles: List<String>,
    private val propertySource: PropertySource,
    private val readProperty: (String) -> String?
) {
    operator fun get(name: String) = propertySource[name] ?: readProperty(name)
}

internal fun newPropertyValues(
    propertyReader: (List<String>) -> (String) -> String?
): PropertyValues {
    val propertySource = PropertySource()
    loadFromEnvironmentVariables(propertySource)
    loadFromSystemProperties(propertySource)
    val profiles = activeProfiles(propertySource)
    return PropertyValues(
        profiles,
        propertySource,
        propertyReader(profiles)
    )
}

private fun activeProfiles(propertySource: PropertySource): List<String> {
    return propertySource[ACTIVE_PROFILES]
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()
}

private fun loadFromEnvironmentVariables(propertySource: PropertySource) {
    val env = System.getenv()
    env.keys.forEach { key ->
        try {
            propertySource[key] = env[key].toString()
        } catch (ignore: PropertyNameException) {
            logger.warn { "Skipping malformed environment variable name: '$key'" }
        }
    }
}

private fun loadFromSystemProperties(propertySource: PropertySource) {
    System.getProperties().forEach { e ->
        propertySource[e.key.toString()] = e.value.toString()
    }
}

