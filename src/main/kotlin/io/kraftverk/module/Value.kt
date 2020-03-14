package io.kraftverk.module

import io.kraftverk.binding.Value
import io.kraftverk.common.ValueConfig
import io.kraftverk.definition.ValueDefinition
import io.kraftverk.internal.container.createValue
import io.kraftverk.internal.container.createValueInstance
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

inline fun <reified T : Any> Module.value(
    name: String? = null,
    default: T? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    noinline instance: ValueDefinition.(Any) -> T
): ValueComponent<T> =
    value(
        name,
        T::class,
        default,
        lazy,
        secret,
        instance
    )

fun <T : Any> Module.value(
    name: String? = null,
    type: KClass<T>,
    default: T? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    instance: ValueDefinition.(Any) -> T

): ValueComponent<T> =
    object : ValueComponent<T> {

        override fun provideDelegate(
            thisRef: Module,
            property: KProperty<*>
        ): ReadOnlyProperty<Module, Value<T>> {
            val valueName = qualifyName(name ?: property.name).toSpinalCase()
            logger.debug { "Creating value '$valueName'" }
            val config = ValueConfig(
                name = valueName,
                lazy = lazy ?: container.lazy,
                secret = secret,
                type = type,
                instance = {
                    container.createValueInstance(valueName, default, instance)
                }
            )
            return container.createValue(config).let(::Delegate)
        }
    }

private val spinalRegex = "([A-Z]+)".toRegex()
private fun String.toSpinalCase() = replace(spinalRegex, "\\-$1").toLowerCase()
