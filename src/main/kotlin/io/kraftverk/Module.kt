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

    internal val moduleContext = ModuleContext()

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

inline fun <reified T : Any> Module.bean(
    lazy: Boolean? = null,
    noinline create: BeanDefinition.() -> T
): DelegateProvider<Module, Bean<T>> =
    newBean(T::class, lazy, create)

fun Module.property(
    name: String? = null,
    defaultValue: String? = null,
    lazy: Boolean? = null
): DelegateProvider<Module, Property> =
    moduleContext.newProperty(name, defaultValue, lazy)

fun <M : Module> Module.module(
    module: () -> M
): DelegateProvider<Module, M> =
    module(name = null, module = module, configure = {})

fun <M : Module> Module.module(
    module: () -> M,
    configure: M.() -> Unit

): DelegateProvider<Module, M> =
    module(name = null, module = module, configure = configure)

fun <M : Module> Module.module(
    name: String,
    module: () -> M
): DelegateProvider<Module, M> =
    module(name = name, module = module, configure = {})

fun <M : Module> Module.module(
    name: String?,
    module: () -> M,
    configure: M.() -> Unit
): DelegateProvider<Module, M> =
    moduleContext.module(name, module, configure)

fun <T : Any> Module.bind(bean: Bean<T>) = BindBean(bean)

fun Module.bind(property: Property) = BindProperty(property)

fun <T : Any> Module.onStart(bean: Bean<T>, block: ConsumerDefinition<T>.(T) -> Unit) {
    bean.onStart(moduleContext.appContext, block)
}

fun <T : Any> Module.onStop(bean: Bean<T>, block: ConsumerDefinition<T>.(T) -> Unit) {
    bean.onStop(moduleContext.appContext, block)
}

fun Module.useProfiles(vararg profiles: String) {
    moduleContext.appContext.setProperty(ACTIVE_PROFILES, profiles.joinToString())
}

@PublishedApi
internal fun <T : Any> Module.newBean(
    type: KClass<T>,
    lazy: Boolean? = null,
    create: BeanDefinition.() -> T
): DelegateProvider<Module, Bean<T>> =
    moduleContext.newBean(
        type,
        lazy,
        create
    )

interface DelegateProvider<in R, out T> {
    operator fun provideDelegate(thisRef: R, prop: KProperty<*>): ReadOnlyProperty<R, T>
}
