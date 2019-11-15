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
    with(Properties()) {
        loadFromClassPathFile(Paths.get("$filename.properties"))
        profiles.forEach {
            loadFromClassPathFile(Paths.get("$filename-$it.properties"))
        }
        forEach { e ->
            props[e.key.toString()] = e.value.toString()
        }
    }
}

private fun Properties.loadFromClassPathFile(path: Path) {
    path.let(Path::toString)
        .let(BindingState::class.java.classLoader::getResourceAsStream)
        ?.also {
            it.use { input -> load(input) }
        }
}
