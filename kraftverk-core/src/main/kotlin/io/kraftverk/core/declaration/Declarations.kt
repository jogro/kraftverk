/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.core.declaration

import io.kraftverk.core.binding.Bean
import io.kraftverk.core.binding.Value
import io.kraftverk.core.binding.delegate
import io.kraftverk.core.common.BindingRef
import io.kraftverk.core.common.Pipe
import io.kraftverk.core.common.delegate
import io.kraftverk.core.env.Environment
import io.kraftverk.core.internal.container.Container
import io.kraftverk.core.internal.container.beanProviders
import io.kraftverk.core.internal.container.valueProviders
import io.kraftverk.core.internal.misc.Supplier
import io.kraftverk.core.provider.get

open class ValueDeclaration internal constructor(internal val container: Container) {
    val env: Environment get() = container.environment
    val valueProviders get() = container.valueProviders
    operator fun <T : Any> Value<T>.invoke(): T = delegate.provider.get()
}

class ValueSupplierDeclaration<T> internal constructor(
    container: Container,
    private val supply: Supplier<T>
) : ValueDeclaration(container) {
    fun callOriginal() = supply()
}

open class BeanDeclaration internal constructor(
    val ctx: BeanDeclarationContext,
    container: Container
) : ValueDeclaration(container) {
    val beanProviders get() = container.beanProviders

    operator fun <T : Any> Bean<T>.invoke(): T = delegate.provider.get()

    operator fun <T : Any> BindingRef<T>.invoke(): T = instance()

    operator fun <T : Any> Pipe<T>.invoke(t: T): T {
        delegate.onPipe(t)
        return t
    }
}

class BeanSupplierInterceptorDeclaration<T> internal constructor(
    ctx: BeanDeclarationContext,
    container: Container,
    private val supply: Supplier<T>
) : BeanDeclaration(ctx, container) {
    fun callOriginal() = supply()
}

class BeanDeclarationContext() {
    internal val lifecycleActions = LifecycleActions()
}

class LifecycleActions {

    @Volatile
    internal var onCreate: () -> Unit = { }

    @Volatile
    internal var onDestroy: () -> Unit = { }

    fun onCreate(block: Action.() -> Unit) {
        val callOriginal = onCreate
        onCreate = { Action(callOriginal).block() }
    }

    fun onDestroy(block: Action.() -> Unit) {
        val callOriginal = onDestroy
        onDestroy = { Action(callOriginal).block() }
    }

    class Action(private val callOriginalFun: () -> Unit) {
        fun callOriginal() {
            callOriginalFun()
        }
    }
}

class PipeDeclaration internal constructor(
    container: Container
) : ValueDeclaration(container) {

    val beanProviders get() = container.beanProviders

    operator fun <T : Any> Bean<T>.invoke(): T = delegate.provider.get()

    operator fun <T : Any> BindingRef<T>.invoke(): T = instance()
}

class BeanConfigurationDeclaration internal constructor(
    container: Container,
    ctx: BeanDeclarationContext
) : BeanDeclaration(ctx, container) {

    fun lifecycle(block: LifecycleActions.() -> Unit) {
        ctx.lifecycleActions.block()
    }
}

open class CustomBeanDeclaration(parent: BeanDeclaration) : BeanDeclaration(parent.ctx, parent.container)
open class CustomValueDeclaration(parent: ValueDeclaration) : ValueDeclaration(parent.container)
