/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.env

import io.kraftverk.internal.logging.createLogger
import java.util.concurrent.ConcurrentHashMap

class ValueSource {
    internal val map = ConcurrentHashMap<String, Any>()

    operator fun get(name: String) = map[name.normalize()]
    operator fun set(name: String, value: Any) {
        if (!name.trim().isValidPropertyName()) {
            throw ValueNameException("Invalid property name: '$name'")
        }
        map[name.normalize()] = value
    }

    companion object
}

const val KRAFTVERK_PROFILES = "kraftverk.active.profiles"

class ValueNameException(msg: String) : Exception(msg)

private val logger = createLogger { }

fun ValueSource.Companion.fromSystem() = ValueSource().apply {
    logger.info { "Loading environment variables (Enable trace level to see actual entries)" }
    System.getenv().forEach { e ->
        try {
            this[e.key] = e.value
            logger.trace { e.key + "=" + e.value }
        } catch (ignore: ValueNameException) {
            logger.warn { "Skipping malformed environment variable name: '${e.key}'" }
        }
    }
    logger.info { "Loaded environment variables" }
    logger.info { "Loading system properties (Enable trace level to see actual entries)" }
    System.getProperties().forEach { e ->
        try {
            this[e.key.toString()] = e.value.toString()
            logger.trace { e.key.toString() + "=" + e.value.toString() }
        } catch (ignore: ValueNameException) {
            logger.warn { "Skipping malformed system property name: '${e.key}'" }
        }
    }
    logger.info { "Loaded system properties" }
}

fun ValueSource.Companion.fromClasspath(
    filenamePrefix: String,
    profiles: List<String>,
    parsers: List<ValueParser>
): List<ValueSource> =
    profiles.map { "$filenamePrefix-$it" }.flatMap { filename ->
        ValueSource.fromClasspath(filename, parsers)
    }

fun ValueSource.Companion.fromClasspath(
    filename: String,
    parsers: List<ValueParser>
): List<ValueSource> = parsers.mapNotNull { parser ->
    val resource = "$filename${parser.fileSuffix}"
    this::class.java.classLoader.getResource(resource)?.let { url ->
        parser.parse(url)
    }
}

internal fun ValueSource.clear() {
    map.clear()
}

internal fun ValueSource.profiles(): List<String> {
    return map[KRAFTVERK_PROFILES]
        ?.toString()
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()
}

private fun String.normalize() = this.replace('_', '.').filter { it != '-' }.toLowerCase().trim()
private val blacklist = "[._]{2}|[._]\$|^[._]".toRegex()
private val whitelist = "^[a-zA-Z0-9._\\-]+\$".toRegex()
private fun String.isValidPropertyName() = !blacklist.containsMatchIn(this) && whitelist.matches(this)
