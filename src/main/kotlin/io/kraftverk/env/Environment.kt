/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.env

import io.kraftverk.logging.newLogger

open class Environment(val profiles: List<String>, valueSources: List<ValueSource>) {
    private val sources = listOf(ValueSource()) + valueSources

    operator fun get(name: String): Any? = sources.mapNotNull { it[name] }.firstOrNull()

    operator fun set(name: String, value: Any) {
        sources.first()[name] = value
    }

    companion object
}

open class EnvironmentDefinition {
    internal val valueSource = ValueSource()
    val valueParsers: MutableList<ValueParser> = mutableListOf(PropertiesParser())
    var propertyFilenamePrefix: String = "application"
    fun set(name: String, value: String) {
        valueSource[name] = value
    }
}

private val logger = newLogger { }

fun environment(
    vararg profiles: String,
    block: EnvironmentDefinition.() -> Unit = {}
): Environment {
    val startMs = System.currentTimeMillis()
    logger.info { "Creating environment" }
    val systemSource = ValueSource.fromSystem()
    val actualProfiles = if (profiles.isEmpty()) systemSource.activeProfiles() else profiles.toList()
    logger.info { "Using profiles: $actualProfiles" }
    val definition = EnvironmentDefinition().apply(block)
    val sources = mutableListOf<ValueSource>()
    sources += definition.valueSource
    sources += systemSource
    sources += ValueSource.fromClasspath(
        definition.propertyFilenamePrefix,
        actualProfiles,
        definition.valueParsers
    )
    sources += ValueSource.fromClasspath(
        definition.propertyFilenamePrefix,
        definition.valueParsers
    )
    return Environment(actualProfiles, sources).also {
        logger.info { "Created environment in ${System.currentTimeMillis() - startMs}ms" }
    }
}
