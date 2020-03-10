package io.kraftverk.env

import io.kraftverk.logging.newLogger
import java.io.InputStream
import java.net.URL
import java.util.Properties

abstract class ValueParser(val extension: String) {
    abstract fun parse(url: URL, values: ValueSource)
}

private val logger = newLogger { }

class PropertiesParser(extension: String = ".properties") : ValueParser(extension) {

    override fun parse(url: URL, values: ValueSource) {
        logger.info { "Loading properties from $url" }
        url.openStream().apply {
            use { inputStream ->
                readProperties(inputStream, values)
            }
        }
    }

    private fun readProperties(inputStream: InputStream, values: ValueSource) {
        Properties().apply {
            load(inputStream)
            forEach { e ->
                values[e.key.toString()] = e.value
                logger.debug { e.key.toString() + "=" + e.value.toString() }
            }
        }
    }
}
