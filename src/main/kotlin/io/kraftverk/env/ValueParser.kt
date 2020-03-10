package io.kraftverk.env

import io.kraftverk.internal.logging.createLogger
import java.io.InputStream
import java.net.URL
import java.util.Properties
import org.yaml.snakeyaml.Yaml

interface ValueParser {
    val fileSuffix: String
    fun parse(url: URL): ValueSource
}

abstract class AbstractValueParser : ValueParser {

    private val logger = createLogger { }

    override fun parse(url: URL): ValueSource {
        logger.info { "Loading values from $url" }
        val valueSource = ValueSource()
        url.openStream().apply {
            use { inputStream ->
                valueSource.readFrom(inputStream)
            }
        }
        logger.info { "Loaded values from $url" }
        return valueSource
    }

    abstract fun ValueSource.readFrom(inputStream: InputStream)
}

class PropertiesParser(override val fileSuffix: String = ".properties") : AbstractValueParser() {

    private val logger = createLogger { }

    override fun ValueSource.readFrom(inputStream: InputStream) {
        val properties = Properties()
        properties.load(inputStream)
        properties.forEach { e ->
            this[e.key.toString()] = e.value
            logger.info { e.key.toString() + "=" + e.value }
        }
    }
}

class YamlParser(override val fileSuffix: String = ".yaml") : AbstractValueParser() {

    private val logger = createLogger { }

    override fun ValueSource.readFrom(inputStream: InputStream) =
        Yaml().loadAll(inputStream)
            .filterIsInstance<Map<*, *>>()
            .forEach { obj -> walk("", obj) }

    private fun ValueSource.walk(path: String, obj: Map<*, *>) {
        obj.forEach { (key, value) ->
            val name = path + key.toString()
            when (value) {
                is CharSequence, is Number, is List<*> -> {
                    this[name] = value
                    logger.info { "$name=$value (type: ${value::class.simpleName})" }
                }
                is Map<*, *> -> walk("$name.", value)
            }
        }
    }
}
