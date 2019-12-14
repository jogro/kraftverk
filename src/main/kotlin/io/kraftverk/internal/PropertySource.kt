/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.PropertyNameException

internal class PropertySource {

    private val map = mutableMapOf<String, String>()

    operator fun get(name: String): String? = map[normalize(name)]

    operator fun set(name: String, value: String) {
        name.trim().also {
            when {
                isValidPropertyName(it) -> map[normalize(it)] = value
                else -> throw PropertyNameException("Invalid property name: '$it'")
            }
        }
    }

    fun clear() {
        map.clear()
    }

    private fun normalize(name: String) = name.replace('_', '.').filter { it != '-' }.toLowerCase().trim()

    private companion object {
        private val blacklist = "[._]{2}|[._]\$|^[._]".toRegex()
        private val whitelist = "^[a-zA-Z0-9._\\-]+\$".toRegex()
        fun isValidPropertyName(name: String) =
            !blacklist.containsMatchIn(name) && whitelist.matches(name)

    }

}
