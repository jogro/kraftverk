/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.env

import java.nio.file.Path
import java.nio.file.Paths
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import mu.KotlinLogging

class ValueSource {
    internal val map = ConcurrentHashMap<String, String>()

    companion object
}

const val ACTIVE_PROFILES = "kraftverk.active.profiles"

class ValueNameException(msg: String) : Exception(msg)

private val logger = KotlinLogging.logger { }

operator fun ValueSource.get(name: String) = map[name.normalize()]
operator fun ValueSource.set(name: String, value: String) {
    if (!name.trim().isValidPropertyName()) {
        throw ValueNameException("Invalid property name: '$name'")
    }
    map[name.normalize()] = value
}

fun ValueSource.Companion.fromSystem() = ValueSource().apply {
    logger.info { "Loading environment variables" }
    System.getenv().forEach { e ->
        try {
            this[e.key] = e.value
            logger.debug { e.key + "=" + e.value }
        } catch (ignore: ValueNameException) {
            logger.warn { "Skipping malformed environment variable name: '${e.key}'" }
        }
    }
    logger.info { "Loading system properties" }
    System.getProperties().forEach { e ->
        this[e.key.toString()] = e.value.toString()
        logger.debug { e.key.toString() + "=" + e.value.toString() }
    }
}

fun ValueSource.Companion.fromClasspath(filenamePrefix: String, profiles: List<String>) =
    profiles.map { "$filenamePrefix-$it" }
        .map { ValueSource.fromClasspath(it) }
        .toTypedArray()

fun ValueSource.Companion.fromClasspath(filename: String) = ValueSource().apply {
    propertiesFromClasspath(
        this::class,
        Paths.get("$filename.properties")
    ).forEach { e ->
        this[e.key.toString()] = e.value.toString()
        logger.debug { e.key.toString() + "=" + e.value.toString() }
    }
}

fun propertiesFromClasspath(clazz: KClass<*>, path: Path) = Properties().apply {
    path.let(Path::toString)
        .let(clazz.java.classLoader::getResource)
        ?.also { logger.info { "Loading properties from $it" } }
        ?.openStream()
        ?.apply {
            use { stream -> load(stream) }
        }
}

internal fun ValueSource.clear() {
    map.clear()
}

internal fun ValueSource.activeProfiles(): List<String> {
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
