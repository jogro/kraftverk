/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.declaration

import io.kraftverk.binding.Bean
import io.kraftverk.binding.BeanImpl
import io.kraftverk.binding.Component
import io.kraftverk.binding.ComponentImpl
import io.kraftverk.binding.Value
import io.kraftverk.binding.ValueImpl
// import io.kraftverk.binding.provider
import io.kraftverk.common.BeanRef
import io.kraftverk.common.ComponentRef
import io.kraftverk.common.ModuleRef
import io.kraftverk.env.Environment
import io.kraftverk.internal.binding.provider
import io.kraftverk.internal.container.Container
import io.kraftverk.internal.container.componentProviders
import io.kraftverk.internal.container.valueProviders
import io.kraftverk.internal.misc.Supplier
import io.kraftverk.module.AbstractModule
import io.kraftverk.provider.get

open class ValueDeclaration internal constructor(internal val container: Container) {
    val env: Environment get() = container.environment
    val valueProviders get() = container.valueProviders
    operator fun <T : Any> Value<T>.invoke(): T = when (this) {
        is ValueImpl<T> -> handler.provider.get()
    }
}

class ValueSupplierDeclaration<T> internal constructor(
    container: Container,
    private val supply: Supplier<T>
) : ValueDeclaration(container) {
    fun proceed() = supply()
}

open class ComponentDeclaration internal constructor(container: Container) : ValueDeclaration(container) {
    val beanProviders get() = container.componentProviders
    operator fun <T : Any, S : Any> Component<T, S>.invoke(): T = when (this) {
        is ComponentImpl<T, S> -> handler.provider.get()
    }
    operator fun <T : Any> Bean<T>.invoke(): T = when (this) {
        is BeanImpl<T> -> handler.provider.get()
    }
    operator fun <T : Any> BeanRef<T>.invoke(): T = instance()
    operator fun <T : Any> ComponentRef<T>.invoke(): T = instance()
    operator fun <M : AbstractModule> ModuleRef<M>.invoke(): M = instance()
}

class ComponentSupplierInterceptorDeclaration<T> internal constructor(
    container: Container,
    private val supply: Supplier<T>
) : ComponentDeclaration(container) {
    fun proceed() = supply()
}

class LifecycleActions {

    @Volatile
    internal var onCreate: () -> Unit = { }

    @Volatile
    internal var onDestroy: () -> Unit = { }

    fun onCreate(block: Action.() -> Unit) {
        val proceed = onCreate
        onCreate = { Action(proceed).block() }
    }

    fun onDestroy(block: Action.() -> Unit) {
        val proceed = onDestroy
        onDestroy = { Action(proceed).block() }
    }

    class Action(val proceed: () -> Unit)
}

class ComponentShapingDeclaration<T> internal constructor(
    container: Container,
    val instance: T,
    private val lifecycle: LifecycleActions
) : ComponentDeclaration(container) {

    fun lifecycle(block: LifecycleActions.() -> Unit) {
        lifecycle.block()
    }
}

open class CustomComponentDeclaration(parent: ComponentDeclaration) : ComponentDeclaration(parent.container)
open class CustomValueDeclaration(parent: ValueDeclaration) : ValueDeclaration(parent.container)
