package io.kraftverk.internal

import io.kraftverk.PropertyNameException

internal class PropertyValueResolver(
    val profiles: List<String>,
    private val properties: PropertySource,
    private val readProperty: (String) -> String?
) {
    operator fun get(name: String) = properties[name] ?: readProperty(name)
}

internal fun newPropertyValueResolver(
    customizedProperties: Map<String, String> = emptyMap(),
    propertyReader: (List<String>) -> (String) -> String?
): PropertyValueResolver {
    val properties = PropertySource()
    loadFromEnvironmentVariables(properties)
    loadFromSystemProperties(properties)
    customizedProperties.forEach { (k, v) -> properties[k] = v }
    val profiles = activeProfiles(properties)
    return PropertyValueResolver(
        profiles,
        properties,
        propertyReader(profiles)
    )
}

private fun activeProfiles(props: PropertySource): List<String> {
    return props[ACTIVE_PROFILES]
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()
}

private fun loadFromEnvironmentVariables(props: PropertySource) {
    val env = System.getenv()
    env.keys.forEach { key ->
        try {
            props[key] = env[key].toString()
        } catch (ignore: PropertyNameException) {
        }
    }
}

private fun loadFromSystemProperties(props: PropertySource) {
    System.getProperties().forEach { e ->
        props[e.key.toString()] = e.value.toString()
    }
}

