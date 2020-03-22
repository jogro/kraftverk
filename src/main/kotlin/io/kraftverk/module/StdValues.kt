/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.declaration.ValueDeclaration
import java.net.ServerSocket

fun Modular.string(
    name: String? = null,
    default: String? = null,
    secret: Boolean = false,
    block: ValueDeclaration.(String) -> String = { it }
): ValueComponent<String> = value(
    name,
    default,
    secret
) { block(it.toString()) }

fun Modular.int(
    name: String? = null,
    default: Int? = null,
    secret: Boolean = false,
    block: ValueDeclaration.(Int) -> Int = { it }
): ValueComponent<Int> = value(
    name,
    default,
    secret
) { block(it.toString().toInt()) }

fun Modular.long(
    name: String? = null,
    default: Long? = null,
    secret: Boolean = false,
    block: ValueDeclaration.(Long) -> Long = { it }
): ValueComponent<Long> = value(
    name,
    default,
    secret
) { block(it.toString().toLong()) }

fun Modular.boolean(
    name: String? = null,
    default: Boolean? = null,
    secret: Boolean = false,
    block: ValueDeclaration.(Boolean) -> Boolean = { it }
): ValueComponent<Boolean> = value(
    name,
    default,
    secret
) { block(it.toString().toBoolean()) }

fun Modular.port(
    name: String? = null,
    default: Int? = null,
    secret: Boolean = false,
    block: ValueDeclaration.(Int) -> Int = { it }
): ValueComponent<Int> =
    int(name, default, secret) { value ->
        block(
            when (value) {
                0 -> ServerSocket(0).use { it.localPort }
                else -> value
            }
        )
    }
