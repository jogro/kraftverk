/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.PropertySource
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

internal fun loadPropertyFilesFromClasspath(defaultFileName: String, profiles: List<String>): PropertySource {
    val propertySource = PropertySource()
    loadFromClasspath(propertySource, defaultFileName, profiles)
    return propertySource
}

private fun loadFromClasspath(propertySource: PropertySource, filename: String, profiles: List<String>) {
    (listOf("$filename.properties") + profiles.map { "$filename-$it.properties" }).forEach {
        with(Properties()) {
            loadFromClassPathFile(Paths.get(it))
            forEach { e ->
                propertySource[e.key.toString()] = e.value.toString()
            }
        }
    }
}

private fun Properties.loadFromClassPathFile(path: Path) {
    path.let(Path::toString)
        .let(BindingDelegate::class.java.classLoader::getResourceAsStream)
        ?.also {
            it.use { input -> load(input) }
        }
}
