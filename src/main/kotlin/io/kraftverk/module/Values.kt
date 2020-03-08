/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.component.ValueComponent
import io.kraftverk.definition.ValueDefinition
import java.net.ServerSocket

fun Module.string(
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
) { block(it.toString()) }

fun Module.int(
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
) { block(it.toString().toInt()) }

fun Module.long(
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
) { block(it.toString().toLong()) }

fun Module.boolean(
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
) { block(it.toString().toBoolean()) }

fun Module.port(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    block: ValueDefinition.(Int) -> Int = { it }
): ValueComponent<Int> =
    value(name, default, lazy, secret) { value ->
        block(
            when (val port = value.toString().toInt()) {
                0 -> ServerSocket(0).use { it.localPort }
                else -> port
            }
        )
    }
