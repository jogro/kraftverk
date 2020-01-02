/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.*
import java.net.ServerSocket
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class Module : InternalModule() {
    companion object
}

inline fun <reified T : Any> bean(
    lazy: Boolean? = null,
    noinline instance: BeanDefinition.() -> T
): DelegateProvider<Module, Bean<T>> =
    newDelegateProvider(
        BeanConfig(
            T::class,
            lazy,
            instance
        )
    )

fun <M : Module> module(name: String? = null, module: () -> M): DelegateProvider<Module, M> =
    newDelegateProvider(name, module)

inline fun <reified T : Any> property(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    noinline instance: PropertyDefinition.(String) -> T
): DelegateProvider<Module, Property<T>> =
    newDelegateProvider(
        name,
        PropertyConfig(
            T::class,
            default,
            lazy,
            secret,
            instance
        )
    )

fun stringProperty(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false
): DelegateProvider<Module, Property<String>> =
    property(
        name,
        default,
        lazy,
        secret
    ) { it }

fun intProperty(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false
): DelegateProvider<Module, Property<Int>> =
    property(
        name,
        default,
        lazy,
        secret
    ) { it.toInt() }

fun longProperty(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false
): DelegateProvider<Module, Property<Long>> =
    property(
        name,
        default,
        lazy,
        secret
    ) { it.toLong() }

fun booleanProperty(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false
): DelegateProvider<Module, Property<Boolean>> =
    property(
        name,
        default,
        lazy,
        secret
    ) { it.toBoolean() }

fun portProperty(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    block: PropertyDefinition.(Int) -> Int = { it }
) =
    property(name, default, lazy, secret) { value ->
        block(
            when (val port = value.toInt()) {
                0 -> ServerSocket(0).use { it.localPort }
                else -> port
            }
        )
    }

fun <T : Any> Module.bind(bean: Bean<T>) = BindBean(container, bean)

fun <T : Any> Module.bind(property: Property<T>) = BindProperty(container, property)

fun <T : Any> Module.onCreate(bean: Bean<T>, block: BeanConsumerDefinition<T>.(T) -> Unit) {
    bean.onCreate { instance, consumer ->
        BeanConsumerDefinition(container.profiles, instance, consumer).block(instance)
    }
}

fun <T : Any> Module.onDestroy(bean: Bean<T>, block: BeanConsumerDefinition<T>.(T) -> Unit) {
    bean.onDestroy { instance, consumer ->
        BeanConsumerDefinition(container.profiles, instance, consumer).block(instance)
    }
}

interface DelegateProvider<in R, out T> {
    operator fun provideDelegate(thisRef: R, prop: KProperty<*>): ReadOnlyProperty<R, T>
}

class BindBean<T : Any> internal constructor(private val container: Container, private val bean: Bean<T>) {
    infix fun to(block: BeanSupplierDefinition<T>.() -> T) {
        bean.onBind { next ->
            BeanSupplierDefinition(container.profiles, next).block()
        }
    }
}

class BindProperty<T : Any> internal constructor(private val container: Container, private val property: Property<T>) {
    infix fun to(block: PropertySupplierDefinition<T>.() -> T) {
        property.onBind { next ->
            PropertySupplierDefinition(container.profiles, next).block()
        }
    }
}
