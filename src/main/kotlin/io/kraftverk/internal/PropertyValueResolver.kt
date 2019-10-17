package io.kraftverk.internal

internal class PropertyValueResolver(
    val profiles: List<String>,
    private val properties: Map<String, String>,
    private val readProperty: (String) -> String?
) {
    operator fun get(name: String) = properties[name] ?: readProperty(name)
}

internal fun newPropertyValueResolver(
    customizedProperties: Map<String, String> = emptyMap(),
    propertyReader: (List<String>) -> (String) -> String?
): PropertyValueResolver {
    val properties = mutableMapOf<String, String>()
    loadFromEnvironmentVariables(properties)
    loadFromSystemProperties(properties)
    properties.putAll(customizedProperties)
    val profiles = activeProfiles(properties)
    return PropertyValueResolver(
        profiles,
        properties,
        propertyReader(profiles)
    )
}

private fun activeProfiles(props: Map<String, String>): List<String> {
    return props[ACTIVE_PROFILES]
        ?.split(",")
        ?.map { it.trim() }
        ?.filterNot { it.isEmpty() }
        ?: emptyList()
}

private fun loadFromEnvironmentVariables(props: MutableMap<String, String>) {
    val env = System.getenv()
    props.keys.forEach { k ->
        env[k.toEnvName()]?.let {
            props[k] = it
        }
    }
    env.keys.forEach { k ->
        props[k.toPropertyName()] = env[k].toString()
    }
}

private fun loadFromSystemProperties(props: MutableMap<String, String>) {
    System.getProperties().forEach { e ->
        props[e.key.toString()] = e.value.toString()
    }
}

private fun String.toEnvName() = replace(".", "_")
    .replace("-", "")
    .toUpperCase()

private fun String.toPropertyName() = replace("_", ".")
    .toLowerCase()
