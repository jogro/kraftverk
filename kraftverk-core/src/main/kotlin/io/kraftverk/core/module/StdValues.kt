/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.core.module

import io.kraftverk.core.declaration.ValueDeclaration
import java.net.ServerSocket

fun BasicModule<*>.string(
    name: String? = null,
    default: String? = null,
    secret: Boolean = false,
    block: ValueDeclaration.(String) -> String = { it }
): ValueDelegateProvider<String> = value(
    name,
    default,
    secret
) { block(it.toString()) }

fun BasicModule<*>.int(
    name: String? = null,
    default: Int? = null,
    secret: Boolean = false,
    block: ValueDeclaration.(Int) -> Int = { it }
): ValueDelegateProvider<Int> = value(
    name,
    default,
    secret
) { block(it.toString().toInt()) }

fun BasicModule<*>.long(
    name: String? = null,
    default: Long? = null,
    secret: Boolean = false,
    block: ValueDeclaration.(Long) -> Long = { it }
): ValueDelegateProvider<Long> = value(
    name,
    default,
    secret
) { block(it.toString().toLong()) }

fun BasicModule<*>.boolean(
    name: String? = null,
    default: Boolean? = null,
    secret: Boolean = false,
    block: ValueDeclaration.(Boolean) -> Boolean = { it }
): ValueDelegateProvider<Boolean> = value(
    name,
    default,
    secret
) { block(it.toString().toBoolean()) }

fun BasicModule<*>.port(
    name: String? = null,
    default: Int? = null,
    secret: Boolean = false,
    block: ValueDeclaration.(Int) -> Int = { it }
): ValueDelegateProvider<Int> =
    int(name, default, secret) { v ->
        when (val value = block(v)) {
            0 -> ServerSocket(0).use { it.localPort }
            else -> value
        }
    }
