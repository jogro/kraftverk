/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.ACTIVE_PROFILES
import mu.KotlinLogging
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

interface Environment {
    val profiles: List<String>
    operator fun get(name: String): String?

    companion object
}

class DefaultEnvironment(override val profiles: List<String>, vararg valueSources: ValueSource) : Environment {
    val sources = listOf(ValueSource(), *valueSources)
    override fun get(name: String): String? = sources.mapNotNull { it[name] }.firstOrNull()

    companion object
}

class ValueSource() {
    internal val map = ConcurrentHashMap<String, String>()

    companion object
}

private val logger = KotlinLogging.logger { }

fun Environment.Companion.withProfiles(vararg profiles: String) = standard(profiles.toList())

fun Environment.Companion.standard(
    profiles: List<String>? = null,
    propertyFilenamePrefix: String = "application"
): DefaultEnvironment {
    val systemSource = ValueSource.fromSystem()
    val actualProfiles = profiles ?: systemSource.activeProfiles()
    return DefaultEnvironment(
        actualProfiles,
        systemSource,
        *ValueSource.fromClasspath(propertyFilenamePrefix, actualProfiles),
        ValueSource.fromClasspath(propertyFilenamePrefix)
    )
}

operator fun DefaultEnvironment.set(name: String, value: String) {
    sources.first()[name] = value
}

operator fun ValueSource.get(name: String) = map[name.normalize()]

operator fun ValueSource.set(name: String, value: String) {
    if (!name.trim().isValidPropertyName()) {
        throw ValueNameException("Invalid property name: '$name'")
    }
    map[name.normalize()] = value
}

fun ValueSource.Companion.fromSystem() = ValueSource().apply {
    System.getenv().forEach { e ->
        try {
            this[e.key] = e.value
        } catch (ignore: ValueNameException) {
            logger.warn { "Skipping malformed environment variable name: '${e.key}'" }
        }
    }
    System.getProperties().forEach { e ->
        this[e.key.toString()] = e.value.toString()
    }
}

fun ValueSource.Companion.fromClasspath(filenamePrefix: String, profiles: List<String>) =
    profiles.map { "$filenamePrefix-$it" }
        .map { ValueSource.fromClasspath(it) }
        .toTypedArray()

fun ValueSource.Companion.fromClasspath(filename: String) = ValueSource().apply {
    propertiesFromClasspath(this::class, Paths.get("$filename.properties")).forEach { e ->
        this[e.key.toString()] = e.value.toString()
    }
}

fun propertiesFromClasspath(clazz: KClass<*>, path: Path) = Properties().apply {
    path.let(Path::toString)
        .let(clazz.java.classLoader::getResourceAsStream)
        ?.apply {
            use { stream -> load(stream) }
        }
}

internal fun ValueSource.clear() {
    map.clear()
}

private fun ValueSource.activeProfiles(): List<String> {
    return map[ACTIVE_PROFILES]
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()
}


private fun String.normalize() = this.replace('_', '.').filter { it != '-' }.toLowerCase().trim()

private val blacklist = "[._]{2}|[._]\$|^[._]".toRegex()
private val whitelist = "^[a-zA-Z0-9._\\-]+\$".toRegex()

private fun String.isValidPropertyName() = !blacklist.containsMatchIn(this) && whitelist.matches(this)