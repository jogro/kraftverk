/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.Bean
import io.kraftverk.Property
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal sealed class AppContextState

internal class AppContextConfiguration : AppContextState() {
    val customizedPropertyValues = mutableMapOf<String, String>()
    val properties = mutableListOf<Property<*>>()
    val beans = mutableListOf<Bean<*>>()
}

internal class PreparedAppContext(
    val propertyValueResolver: PropertyValueResolver,
    val properties: List<Property<*>>,
    val beans: List<Bean<*>>
) : AppContextState()

internal object DestroyedAppContext : AppContextState()

internal inline fun <reified T : AppContextState> AppContextState.runIf(block: T.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (this is T) {
        this.block()
    }
}

internal inline fun <reified T : AppContextState> AppContextState.runAs(block: T.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    if (this is T) {
        this.block()
    } else {
        throw IllegalStateException("Expected state to be ${T::class} but was ${this::class}")
    }
}
