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

inline fun <reified T : Any> value(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    noinline instance: ValueDefinition.(String) -> T
): DelegateProvider<Module, Value<T>> =
    newDelegateProvider(
        name,
        ValueConfig(
            T::class,
            default,
            lazy,
            secret,
            instance
        )
    )

fun stringValue(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    block: ValueDefinition.(String) -> String = { it }
): DelegateProvider<Module, Value<String>> =
    value(
        name,
        default,
        lazy,
        secret
    ) { block(it) }

fun intValue(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    block: ValueDefinition.(Int) -> Int = { it }
): DelegateProvider<Module, Value<Int>> =
    value(
        name,
        default,
        lazy,
        secret
    ) { block(it.toInt()) }

fun longValue(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    block: ValueDefinition.(Long) -> Long = { it }
): DelegateProvider<Module, Value<Long>> =
    value(
        name,
        default,
        lazy,
        secret
    ) { block(it.toLong()) }

fun booleanValue(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    block: ValueDefinition.(Boolean) -> Boolean = { it }
): DelegateProvider<Module, Value<Boolean>> =
    value(
        name,
        default,
        lazy,
        secret
    ) { block(it.toBoolean()) }

fun portValue(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    block: ValueDefinition.(Int) -> Int = { it }
) =
    value(name, default, lazy, secret) { value ->
        block(
            when (val port = value.toInt()) {
                0 -> ServerSocket(0).use { it.localPort }
                else -> port
            }
        )
    }

fun <T : Any> Module.bind(bean: Bean<T>) = BindBean(container, bean)

fun <T : Any> Module.bind(value: Value<T>) = BindProperty(container, value)

fun <T : Any> Module.onCreate(bean: Bean<T>, block: BeanConsumerDefinition<T>.(T) -> Unit) {
    bean.onCreate { instance, consumer ->
        BeanConsumerDefinition(container.environment, instance, consumer).block(instance)
    }
}

fun <T : Any> Module.onDestroy(bean: Bean<T>, block: BeanConsumerDefinition<T>.(T) -> Unit) {
    bean.onDestroy { instance, consumer ->
        BeanConsumerDefinition(container.environment, instance, consumer).block(instance)
    }
}

interface DelegateProvider<in R, out T> {
    operator fun provideDelegate(thisRef: R, prop: KProperty<*>): ReadOnlyProperty<R, T>
}

class BindBean<T : Any> internal constructor(private val container: Container, private val bean: Bean<T>) {
    infix fun to(block: BeanSupplierDefinition<T>.() -> T) {
        bean.onBind { next ->
            BeanSupplierDefinition(container.environment, next).block()
        }
    }
}

class BindProperty<T : Any> internal constructor(private val container: Container, private val value: Value<T>) {
    infix fun to(block: ValueSupplierDefinition<T>.() -> T) {
        value.onBind { next ->
            ValueSupplierDefinition(container.environment, next).block()
        }
    }
}
