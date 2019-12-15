/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

class PropertySource {

    private val map = mutableMapOf<String, String>()

    operator fun get(name: String): String? = map[name.trim().normalize()]

    operator fun set(name: String, value: String) {
        val trimmed = name.trim()
        when {
            trimmed.isValidPropertyName() -> map[trimmed.normalize()] = value
            else -> throw PropertyNameException("Invalid property name: '$name'")
        }
    }

    internal fun clear() {
        map.clear()
    }

    private fun String.normalize() = this.replace('_', '.').filter { it != '-' }.toLowerCase().trim()

    private companion object {
        private val blacklist = "[._]{2}|[._]\$|^[._]".toRegex()
        private val whitelist = "^[a-zA-Z0-9._\\-]+\$".toRegex()
        fun String.isValidPropertyName() = !blacklist.containsMatchIn(this) && whitelist.matches(this)
    }

}
