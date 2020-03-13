package io.kraftverk.module

import io.kraftverk.binding.Value
import io.kraftverk.definition.ValueDefinition
import io.kraftverk.internal.binding.ValueConfig
import io.kraftverk.internal.container.createValue
import io.kraftverk.internal.container.createValueInstance
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// TODO Make this configurable
private val valueParamsProcessor = defaultValueParamsProcessor()

inline fun <reified T : Any> Module.value(
    name: String? = null,
    default: T? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    noinline instance: ValueDefinition.(Any) -> T
): ValueComponent<T> =
    ValueParams(
        name,
        T::class,
        default,
        lazy,
        secret,
        instance
    ).let(::createValueComponent)

@PublishedApi
internal fun <T : Any> Module.createValueComponent(params: ValueParams<T>): ValueComponent<T> =
    object : ValueComponent<T> {

        override fun provideDelegate(
            thisRef: Module,
            property: KProperty<*>
        ): ReadOnlyProperty<Module, Value<T>> {

            val processed = valueParamsProcessor.process(namespace, property.name, params)

            val valueName = qualifyName(processed.name ?: property.name).toSpinalCase()
            logger.debug { "Creating value '$valueName'" }
            val config = ValueConfig(
                name = valueName,
                lazy = processed.lazy ?: container.lazy,
                secret = processed.secret,
                type = processed.type,
                instance = {
                    container.createValueInstance(valueName, processed.default, processed.instance)
                }
            )
            return container.createValue(config).let(::Delegate)
        }
    }

private val spinalRegex = "([A-Z]+)".toRegex()
private fun String.toSpinalCase() = replace(spinalRegex, "\\-$1").toLowerCase()
