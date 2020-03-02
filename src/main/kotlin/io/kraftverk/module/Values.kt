package io.kraftverk.module

import io.kraftverk.component.ValueComponent
import io.kraftverk.definition.ValueDefinition
import io.kraftverk.internal.component.ValueConfig
import io.kraftverk.internal.component.newValueComponent
import java.net.ServerSocket

inline fun <reified T : Any> value(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    noinline instance: ValueDefinition.(String) -> T
): ValueComponent<T> =
    newValueComponent(
        name,
        ValueConfig(
            T::class,
            default,
            lazy,
            secret,
            instance
        )
    )

fun string(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    block: ValueDefinition.(String) -> String = { it }
): ValueComponent<String> = value(
    name,
    default,
    lazy,
    secret
) { block(it) }

fun int(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    block: ValueDefinition.(Int) -> Int = { it }
): ValueComponent<Int> = value(
    name,
    default,
    lazy,
    secret
) { block(it.toInt()) }

fun long(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    block: ValueDefinition.(Long) -> Long = { it }
): ValueComponent<Long> = value(
    name,
    default,
    lazy,
    secret
) { block(it.toLong()) }

fun boolean(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    block: ValueDefinition.(Boolean) -> Boolean = { it }
): ValueComponent<Boolean> = value(
    name,
    default,
    lazy,
    secret
) { block(it.toBoolean()) }

fun port(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    block: ValueDefinition.(Int) -> Int = { it }
): ValueComponent<Int> =
    value(name, default, lazy, secret) { value ->
        block(
            when (val port = value.toInt()) {
                0 -> ServerSocket(0).use { it.localPort }
                else -> port
            }
        )
    }
