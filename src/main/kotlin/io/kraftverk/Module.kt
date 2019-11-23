/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Basic component to inherit from when creating a module.
 */
abstract class Module {

    private val moduleContext = ModuleContext()

    protected inline fun <reified T : Any> bean(
        lazy: Boolean? = null,
        noinline create: BeanDefinition.() -> T
    ): DelegateProvider<Module, Bean<T>> =
        newBean(T::class, lazy, create)

    protected fun property(
        name: String? = null,
        defaultValue: String? = null,
        lazy: Boolean? = null
    ): DelegateProvider<Module, Property> =
        moduleContext.newProperty(name, defaultValue, lazy)

    protected fun <M : Module> module(
        module: () -> M
    ): DelegateProvider<Module, M> =
        module(name = null, module = module, configure = {})

    protected fun <M : Module> module(
        module: () -> M,
        configure: M.() -> Unit

    ): DelegateProvider<Module, M> =
        module(name = null, module = module, configure = configure)

    protected fun <M : Module> module(
        name: String,
        module: () -> M
    ): DelegateProvider<Module, M> =
        module(name = name, module = module, configure = {})

    protected fun <M : Module> module(
        name: String?,
        module: () -> M,
        configure: M.() -> Unit
    ): DelegateProvider<Module, M> =
        moduleContext.module(name, module, configure)

    fun useProfiles(vararg profiles: String) {
        moduleContext.appContext.setProperty(ACTIVE_PROFILES, profiles.joinToString())
    }

    fun <T : Any> bind(bean: Bean<T>) = BindBean(bean)

    fun bind(property: Property) = BindProperty(property)

    fun <T : Any> onStart(bean: Bean<T>, block: ConsumerDefinition<T>.(T) -> Unit) {
        bean.onStart(moduleContext.appContext, block)
    }

    fun <T : Any> onStop(bean: Bean<T>, block: ConsumerDefinition<T>.(T) -> Unit) {
        bean.onStop(moduleContext.appContext, block)
    }

    @PublishedApi
    internal fun <T : Any> newBean(
        type: KClass<T>,
        lazy: Boolean? = null,
        create: BeanDefinition.() -> T
    ): DelegateProvider<Module, Bean<T>> =
        moduleContext.newBean(
            type,
            lazy,
            create
        )

    inner class BindBean<T : Any>(private val bean: Bean<T>) {
        infix fun to(block: SupplierDefinition<T>.() -> T) {
            bean.onSupply(moduleContext.appContext, block)
        }
    }

    inner class BindProperty(private val property: Property) {
        infix fun to(block: () -> String) {
            property.onSupply(moduleContext.appContext, block)
        }
    }

}

interface DelegateProvider<in R, out T> {
    operator fun provideDelegate(thisRef: R, prop: KProperty<*>): ReadOnlyProperty<R, T>
}
