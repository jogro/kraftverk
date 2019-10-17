/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.*
import io.kraftverk.PropertyImpl
import io.kraftverk.PrototypeBeanImpl
import io.kraftverk.SingletonBeanImpl
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

private fun <T : Any> Bean<T>.toBinding(): Binding<T> = when (this) {
    is SingletonBeanImpl<T> -> this.binding
    is PrototypeBeanImpl<T> -> this.binding
}

private fun Property.toBinding(): PropertyBinding = when (this) {
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

internal fun <T : Any> SingletonBean<T>.onStop(
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

internal fun Property.onSupply(
    appContext: AppContext,
    value: () -> String
) {
    val block: SupplierDefinition<String>.() -> String = { value() }
    this.toBinding().onSupply(appContext, block)
}

internal fun Property.initialize() {
    this.toBinding().initialize()
}

internal fun Property.start() {
    this.toBinding().start()
}

internal fun Bean<*>.destroy() {
    this.toBinding().destroy()
}

internal fun <T : Any> Bean<T>.provider(): Provider<T> {
    return this.toBinding().provider()
}

internal fun Property.provider(): Provider<String> {
    return this.toBinding().provider()
}
