/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

internal fun loadPropertyFilesFromClasspath(defaultFileName: String, profiles: List<String>): Map<String, String> {
    val props = mutableMapOf<String, String>()
    loadFromClasspath(props, defaultFileName, profiles)
    return props
}

private fun loadFromClasspath(props: MutableMap<String, String>, filename: String, profiles: List<String>) {
    val loaded = Properties()
    loadFromClassPathFile(loaded, Paths.get("$filename.properties"))
    profiles.forEach {
        loadFromClassPathFile(loaded, Paths.get("$filename-$it.properties"))
    }
    loaded.forEach { e ->
        props[e.key.toString()] = e.value.toString()
    }
}

private fun loadFromClassPathFile(props: Properties, path: Path) {
    path.let(Path::toString)
        .let(BindingState::class.java.classLoader::getResourceAsStream)
        ?.let {
            it.use { input -> props.load(input) }
        }
}
