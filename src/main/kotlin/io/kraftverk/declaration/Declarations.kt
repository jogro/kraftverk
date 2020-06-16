/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.declaration

import io.kraftverk.binding.Bean
import io.kraftverk.binding.BeanImpl
import io.kraftverk.binding.Value
import io.kraftverk.binding.ValueImpl
import io.kraftverk.common.BeanRef
import io.kraftverk.common.ModuleRef
import io.kraftverk.env.Environment
import io.kraftverk.internal.binding.provider
import io.kraftverk.internal.container.Container
import io.kraftverk.internal.container.beanProviders
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

open class BeanDeclaration internal constructor(container: Container) : ValueDeclaration(container) {
    val beanProviders get() = container.beanProviders

    operator fun <T : Any, S : Any> Bean<T, S>.invoke(): T = when (this) {
        is BeanImpl<T, S> -> handler.provider.get()
    }

    operator fun <T : Any> BeanRef<T>.invoke(): T = instance()
    operator fun <M : AbstractModule> ModuleRef<M>.invoke(): M = instance()
}

class BeanSupplierInterceptorDeclaration<T> internal constructor(
    container: Container,
    private val supply: Supplier<T>
) : BeanDeclaration(container) {
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

class BeanShapingDeclaration<T> internal constructor(
    container: Container,
    val instance: T,
    private val lifecycle: LifecycleActions
) : BeanDeclaration(container) {

    fun lifecycle(block: LifecycleActions.() -> Unit) {
        lifecycle.block()
    }
}

open class CustomBeanDeclaration(parent: BeanDeclaration) : BeanDeclaration(parent.container)
open class CustomValueDeclaration(parent: ValueDeclaration) : ValueDeclaration(parent.container)
