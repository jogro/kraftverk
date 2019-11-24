/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

private fun <T : Any> Bean<T>.toBinding(): Binding<T, Provider<T>> = when (this) {
    is BeanImpl<T> -> binding
}

internal fun <T : Any> Bean<T>.instanceId(): Int? = when (this) {
    is BeanImpl<T> -> this.binding.provider().instanceId
}

private fun <T : Any> Property<T>.toBinding(): Binding<T, Provider<T>> = when (this) {
    is PropertyImpl -> this.binding
}

internal inline fun <reified T : BindingState<*>> BindingState<*>.on(block: T.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (this is T) {
        this.block()
    }
}

internal inline fun <reified T : BindingState<*>> BindingState<*>.expect(block: T.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    if (this is T) {
        this.block()
    } else {
        throw Exception("Expected state to be ${T::class} but was ${this::class}")
    }
}

internal fun <T : Any> Bean<*>.onSupply(
    appContext: AppContext,
    block: SupplierDefinition<T>.() -> T
) {
    this.toBinding().onSupply(appContext, block)
}

internal fun <T : Any> Bean<T>.onStart(
    appContext: AppContext,
    block: ConsumerDefinition<T>.(T) -> Unit
) {
    this.toBinding().onStart(appContext, block)
}

internal fun <T : Any> Bean<T>.onStop(
    appContext: AppContext,
    block: ConsumerDefinition<T>.(T) -> Unit
) {
    this.toBinding().onStop(appContext, block)
}

internal fun Bean<*>.initialize() {
    this.toBinding().initialize()
}

internal fun Bean<*>.start() {
    this.toBinding().start()
}

internal fun <T : Any> Property<T>.onSupply(
    appContext: AppContext,
    value: () -> T
) {
    val block: SupplierDefinition<T>.() -> T = { value() }
    this.toBinding().onSupply(appContext, block)
}

internal fun <T : Any> Property<T>.initialize() {
    this.toBinding().initialize()
}

internal fun Property<*>.start() {
    this.toBinding().start()
}

internal fun Bean<*>.destroy() {
    this.toBinding().destroy()
}

internal fun <T : Any> Property<T>.provider(): Provider<T> {
    return this.toBinding().provider()
}

internal fun <T : Any> Bean<T>.provider(): Provider<T> {
    return this.toBinding().provider()
}
