/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Value
import io.kraftverk.common.ValueDefinition
import io.kraftverk.declaration.ValueDeclaration
import io.kraftverk.internal.container.createValue
import io.kraftverk.internal.container.createValueInstance
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

inline fun <reified T : Any> AbstractModule.value(
    name: String? = null,
    default: T? = null,
    secret: Boolean = false,
    noinline instance: ValueDeclaration.(Any) -> T
): ValueDelegateProvider<T> =
    value(
        name,
        T::class,
        default,
        secret,
        instance
    )

@PublishedApi
internal fun <T : Any> AbstractModule.value(
    name: String? = null,
    type: KClass<T>,
    default: T? = null,
    secret: Boolean = false,
    instance: ValueDeclaration.(Any) -> T

): ValueDelegateProvider<T> =
    object : ValueDelegateProvider<T> {

        override fun provideDelegate(
            thisRef: AbstractModule,
            property: KProperty<*>
        ): ReadOnlyProperty<AbstractModule, Value<T>> {
            val valueName = qualifyName(name ?: property.name).toSpinalCase()
            logger.debug { "Creating value '$valueName'" }
            val config = ValueDefinition(
                name = valueName,
                lazy = null,
                secret = secret,
                type = type,
                instance = {
                    container.createValueInstance(valueName, default, instance)
                }
            )
            return container.createValue<T>(config).let(::Delegate)
        }
    }

private val spinalRegex = "([A-Z]+)".toRegex()
private fun String.toSpinalCase() = replace(spinalRegex, "\\-$1").toLowerCase()
