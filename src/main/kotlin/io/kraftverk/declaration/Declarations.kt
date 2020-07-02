/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.declaration

import io.kraftverk.binding.Bean
import io.kraftverk.binding.Value
import io.kraftverk.binding.delegate
import io.kraftverk.common.BindingRef
import io.kraftverk.common.Pipe
import io.kraftverk.common.delegate
import io.kraftverk.env.Environment
import io.kraftverk.internal.container.Container
import io.kraftverk.internal.container.beanProviders
import io.kraftverk.internal.container.valueProviders
import io.kraftverk.internal.misc.Supplier
import io.kraftverk.provider.get

open class ValueDeclaration internal constructor(internal val container: Container) {
    val env: Environment get() = container.environment
    val valueProviders get() = container.valueProviders
    operator fun <T : Any> Value<T>.invoke(): T = delegate.provider.get()
}

class ValueSupplierDeclaration<T> internal constructor(
    container: Container,
    private val supply: Supplier<T>
) : ValueDeclaration(container) {
    fun proceed() = supply()
}

open class BeanDeclaration internal constructor(
    internal val lifecycleActions: LifecycleActions,
    container: Container
) : ValueDeclaration(container) {
    val beanProviders get() = container.beanProviders

    operator fun <T : Any> Bean<T>.invoke(): T = delegate.provider.get()

    operator fun <T : Any> BindingRef<T>.invoke(): T = instance()

    operator fun <T : Any> Pipe<T>.invoke(t: T) {
        delegate.onConfigure(t, lifecycleActions)
    }
}

class BeanSupplierInterceptorDeclaration<T> internal constructor(
    lifecycleActions: LifecycleActions,
    container: Container,
    private val supply: Supplier<T>
) : BeanDeclaration(lifecycleActions, container) {
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

    class Action(private val proceedFun: () -> Unit) {
        fun proceed() {
            proceedFun()
        }
    }
}

class PipeDeclaration<T> internal constructor(
    container: Container,
    val instance: T,
    lifecycleActions: LifecycleActions
) : BeanDeclaration(lifecycleActions, container) {

    fun lifecycle(block: LifecycleActions.() -> Unit) {
        lifecycleActions.block()
    }
}

class BeanConfigurationDeclaration<T> internal constructor(
    container: Container,
    val instance: T,
    lifecycleActions: LifecycleActions
) : BeanDeclaration(lifecycleActions, container) {

    fun lifecycle(block: LifecycleActions.() -> Unit) {
        lifecycleActions.block()
    }
}

open class CustomBeanDeclaration(parent: BeanDeclaration) : BeanDeclaration(parent.lifecycleActions, parent.container)
open class CustomValueDeclaration(parent: ValueDeclaration) : ValueDeclaration(parent.container)
