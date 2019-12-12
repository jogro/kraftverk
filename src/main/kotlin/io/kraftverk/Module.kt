/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.*
import java.net.ServerSocket
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
            bean.onBind { next ->
                BeanSupplierDefinition(moduleContext.runtime, next).block()
            }
        }
    }

    inner class BindProperty<T : Any>(private val property: Property<T>) {
        infix fun to(block: PropertySupplierDefinition<T>.() -> T) {
            property.onBind { next ->
                PropertySupplierDefinition(moduleContext.runtime, next).block()
            }
        }
    }

    companion object
}

inline fun <reified T : Any> Module.bean(
    lazy: Boolean? = null,
    noinline instance: BeanDefinition.() -> T
): DelegateProvider<Module, Bean<T>> =
    newBean(
        T::class,
        lazy,
        instance
    )

fun Module.stringProperty(
    name: String? = null,
    defaultValue: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false
): DelegateProvider<Module, Property<String>> =
    property(
        name,
        defaultValue,
        lazy,
        secret
    ) { it }

fun Module.intProperty(
    name: String? = null,
    defaultValue: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false
): DelegateProvider<Module, Property<Int>> =
    property(
        name,
        defaultValue,
        lazy,
        secret
    ) { it.toInt() }

fun Module.longProperty(
    name: String? = null,
    defaultValue: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false
): DelegateProvider<Module, Property<Long>> =
    property(
        name,
        defaultValue,
        lazy,
        secret
    ) { it.toLong() }

fun Module.booleanProperty(
    name: String? = null,
    defaultValue: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false
): DelegateProvider<Module, Property<Boolean>> =
    property(
        name,
        defaultValue,
        lazy,
        secret
    ) { it.toBoolean() }

fun Module.portProperty(
    name: String? = null,
    defaultValue: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    block: PropertyDefinition.(Int) -> Int = { it }
) =
    property(name, defaultValue, lazy, secret) { value ->
        block(
            when (val port = value.toInt()) {
                0 -> ServerSocket(0).use { it.localPort }
                else -> port
            }
        )
    }

inline fun <reified T : Any> Module.property(
    name: String? = null,
    defaultValue: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    noinline instance: PropertyDefinition.(String) -> T
): DelegateProvider<Module, Property<T>> =
    newProperty(
        T::class,
        name,
        defaultValue,
        lazy,
        secret,
        instance
    )

fun <M : Module> Module.module(
    name: String? = null,
    module: () -> M
): DelegateProvider<Module, M> =
    moduleContext.module(name, module)

fun <T : Any> Module.bind(bean: Bean<T>) = BindBean(bean)

fun <T : Any> Module.bind(property: Property<T>) = BindProperty(property)

fun <T : Any> Module.onCreate(bean: Bean<T>, block: BeanConsumerDefinition<T>.(T) -> Unit) {
    bean.onCreate { instance, consumer ->
        BeanConsumerDefinition(moduleContext.runtime, instance, consumer).block(instance)
    }
}

fun <T : Any> Module.onDestroy(bean: Bean<T>, block: BeanConsumerDefinition<T>.(T) -> Unit) {
    bean.onDestroy { instance, consumer ->
        BeanConsumerDefinition(moduleContext.runtime, instance, consumer).block(instance)
    }
}

fun Module.useProfiles(vararg profiles: String) {
    moduleContext.runtime.setProperty(ACTIVE_PROFILES, profiles.joinToString())
}

@PublishedApi
internal fun <T : Any> Module.newBean(
    type: KClass<T>,
    lazy: Boolean? = null,
    instance: BeanDefinition.() -> T
): DelegateProvider<Module, Bean<T>> =
    moduleContext.newBean(
        type,
        lazy,
        instance
    )

@PublishedApi
internal fun <T : Any> Module.newProperty(
    type: KClass<T>,
    name: String?,
    defaultValue: String?,
    lazy: Boolean?,
    secret: Boolean,
    instance: PropertyDefinition.(String) -> T
): DelegateProvider<Module, Property<T>> =
    moduleContext.newProperty(
        type,
        name,
        defaultValue,
        lazy,
        secret,
        instance
    )

interface DelegateProvider<in R, out T> {
    operator fun provideDelegate(thisRef: R, prop: KProperty<*>): ReadOnlyProperty<R, T>
}
