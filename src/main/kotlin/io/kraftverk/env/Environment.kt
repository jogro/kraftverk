/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.env

import mu.KotlinLogging

class Environment(val profiles: List<String>, vararg valueSources: ValueSource) {
    val sources = listOf(ValueSource(), *valueSources)

    companion object
}

operator fun Environment.get(name: String): String? = sources.mapNotNull { it[name] }.firstOrNull()

operator fun Environment.set(name: String, value: String) {
    sources.first()[name] = value
}

class EnvironmentDefinition {
    internal val valueSource = ValueSource()
    var propertyFilenamePrefix: String = "application"
    fun set(name: String, value: String) {
        valueSource[name] = value
    }
}

private val logger = KotlinLogging.logger { }

fun environment(vararg profiles: String, block: EnvironmentDefinition.() -> Unit = {}): Environment {
    val startMs = System.currentTimeMillis()
    logger.info { "Creating environment" }
    val systemSource = ValueSource.fromSystem()
    val actualProfiles = if (profiles.isEmpty()) systemSource.activeProfiles() else profiles.toList()
    logger.info { "Using profiles: $actualProfiles" }
    val definition = EnvironmentDefinition().apply(block)
    return Environment(
        actualProfiles,
        definition.valueSource,
        systemSource,
        *ValueSource.fromClasspath(
            definition.propertyFilenamePrefix,
            actualProfiles
        ),
        ValueSource.fromClasspath(definition.propertyFilenamePrefix)
    ).also {
        logger.info { "Created environment in ${System.currentTimeMillis() - startMs}ms" }
    }
}
