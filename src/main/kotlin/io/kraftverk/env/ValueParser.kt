package io.kraftverk.env

import io.kraftverk.internal.logging.createLogger
import java.io.InputStream
import java.net.URL
import java.util.Properties

abstract class ValueParser(val extension: String) {
    abstract fun parse(url: URL): ValueSource
}

class PropertiesParser(extension: String = ".properties") : ValueParser(extension) {

    private val logger = createLogger { }

    override fun parse(url: URL): ValueSource {
        logger.info { "Loading properties from $url" }
        val valueSource = ValueSource()
        url.openStream().apply {
            use { inputStream ->
                valueSource.readProperties(inputStream)
            }
        }
        logger.info { "Loaded properties from $url" }
        return valueSource
    }

    private fun ValueSource.readProperties(inputStream: InputStream) {
        val properties = Properties()
        properties.load(inputStream)
        properties.forEach { e ->
            this[e.key.toString()] = e.value
            logger.debug { e.key.toString() + "=" + e.value.toString() }
        }
    }
}
