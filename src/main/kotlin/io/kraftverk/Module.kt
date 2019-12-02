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
        infix fun to(block: BeanSupplierDefinition<T>.() -> T) {
            bean.onSupply { next ->
                 BeanSupplierDefinition(moduleContext.appContext, next).block()
            }
        }
    }

    inner class BindProperty<T : Any>(private val property: Property<T>) {
        infix fun to(block: PropertySupplierDefinition<T>.() -> T) {
            property.onSupply { next ->
                PropertySupplierDefinition(moduleContext.appContext, next).block()
            }
        }
    }

}

inline fun <reified T : Any> Module.bean(
    lazy: Boolean? = null,
    noinline define: BeanDefinition.() -> T
): DelegateProvider<Module, Bean<T>> =
    newBean(T::class, lazy, define)

fun Module.property(
    name: String? = null,
    defaultValue: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false
): DelegateProvider<Module, Property<String>> =
    property(name, defaultValue, lazy, secret) { value -> value }

inline fun <reified T : Any> Module.property(
    name: String? = null,
    defaultValue: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    noinline define: PropertyDefinition.(String) -> T
): DelegateProvider<Module, Property<T>> =
    newProperty(T::class, name, defaultValue, lazy, secret, define)

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

fun <T : Any> Module.bind(property: Property<T>) = BindProperty(property)

fun <T : Any> Module.onStart(bean: Bean<T>, block: BeanConsumerDefinition<T>.(T) -> Unit) {
    bean.onStart { instance, consumer ->
        BeanConsumerDefinition(moduleContext.appContext, instance, consumer).block(instance)
    }
}

fun <T : Any> Module.onStop(bean: Bean<T>, block: BeanConsumerDefinition<T>.(T) -> Unit) {
    bean.onStop { instance, consumer ->
        BeanConsumerDefinition(moduleContext.appContext, instance, consumer).block(instance)
    }
}

fun Module.useProfiles(vararg profiles: String) {
    moduleContext.appContext.setProperty(ACTIVE_PROFILES, profiles.joinToString())
}

@PublishedApi
internal fun <T : Any> Module.newBean(
    type: KClass<T>,
    lazy: Boolean? = null,
    define: BeanDefinition.() -> T
): DelegateProvider<Module, Bean<T>> =
    moduleContext.newBean(
        type,
        lazy,
        define
    )

@PublishedApi
internal fun <T : Any> Module.newProperty(
    type: KClass<T>,
    name: String? = null,
    defaultValue: String?,
    lazy: Boolean?,
    secret: Boolean,
    define: PropertyDefinition.(String) -> T
): DelegateProvider<Module, Property<T>> =
    moduleContext.newProperty(
        type = type,
        name = name,
        defaultValue = defaultValue,
        lazy = lazy,
        secret = secret,
        define = define
    )

interface DelegateProvider<in R, out T> {
    operator fun provideDelegate(thisRef: R, prop: KProperty<*>): ReadOnlyProperty<R, T>
}
