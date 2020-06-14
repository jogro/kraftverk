/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.declaration.ValueDeclaration
import java.net.ServerSocket

fun AbstractModule.string(
    name: String? = null,
    default: String? = null,
    secret: Boolean = false,
    block: ValueDeclaration.(String) -> String = { it }
): ValueDelegateProvider<String, String> = value(
    name,
    default,
    secret
) { block(it.toString()) }

fun AbstractModule.int(
    name: String? = null,
    default: Int? = null,
    secret: Boolean = false,
    block: ValueDeclaration.(Int) -> Int = { it }
): ValueDelegateProvider<Int, Int> = value(
    name,
    default,
    secret
) { block(it.toString().toInt()) }

fun AbstractModule.long(
    name: String? = null,
    default: Long? = null,
    secret: Boolean = false,
    block: ValueDeclaration.(Long) -> Long = { it }
): ValueDelegateProvider<Long, Long> = value(
    name,
    default,
    secret
) { block(it.toString().toLong()) }

fun AbstractModule.boolean(
    name: String? = null,
    default: Boolean? = null,
    secret: Boolean = false,
    block: ValueDeclaration.(Boolean) -> Boolean = { it }
): ValueDelegateProvider<Boolean, Boolean> = value(
    name,
    default,
    secret
) { block(it.toString().toBoolean()) }

fun AbstractModule.port(
    name: String? = null,
    default: Int? = null,
    secret: Boolean = false,
    block: ValueDeclaration.(Int) -> Int = { it }
): ValueDelegateProvider<Int, Int> =
    int(name, default, secret) { value ->
        block(
            when (value) {
                0 -> ServerSocket(0).use { it.localPort }
                else -> value
            }
        )
    }
