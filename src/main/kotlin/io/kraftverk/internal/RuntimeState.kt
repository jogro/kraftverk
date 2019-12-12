/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.Bean
import io.kraftverk.Property
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal sealed class RuntimeState

internal class RuntimeConfiguration : RuntimeState() {
    val customizedPropertyValues = mutableMapOf<String, String>()
    val properties = mutableListOf<Property<*>>()
    val beans = mutableListOf<Bean<*>>()
}

internal class PreparedRuntime(
    val propertyValueResolver: PropertyValueResolver,
    val properties: List<Property<*>>,
    val beans: List<Bean<*>>
) : RuntimeState()

internal object DestroyedRuntime : RuntimeState()

internal inline fun <reified T : RuntimeState> RuntimeState.runIf(block: T.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (this is T) {
        this.block()
    }
}

internal inline fun <reified T : RuntimeState> RuntimeState.runAs(block: T.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    if (this is T) {
        this.block()
    } else {
        throw IllegalStateException("Expected state to be ${T::class} but was ${this::class}")
    }
}
