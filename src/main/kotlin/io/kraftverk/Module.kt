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
    refreshable: Boolean? = null,
    noinline instance: BeanDefinition.() -> T
): BeanDelegate<T> = newBeanDelegate(
    BeanConfig(
        T::class,
        lazy,
        refreshable,
        instance
    )
)

fun <M : Module> module(
    name: String? = null,
    module: () -> M
): ModuleDelegate<M> = newModuleDelegate(
    name,
    module
)

inline fun <reified T : Any> value(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    noinline instance: ValueDefinition.(String) -> T
): ValueDelegate<T> = newValueDelegate(
    name,
    ValueConfig(
        T::class,
        default,
        lazy,
        secret,
        instance
    )
)

fun string(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    block: ValueDefinition.(String) -> String = { it }
): ValueDelegate<String> = value(
    name,
    default,
    lazy,
    secret
) { block(it) }

fun int(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    block: ValueDefinition.(Int) -> Int = { it }
): ValueDelegate<Int> = value(
    name,
    default,
    lazy,
    secret
) { block(it.toInt()) }

fun long(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    block: ValueDefinition.(Long) -> Long = { it }
): ValueDelegate<Long> = value(
    name,
    default,
    lazy,
    secret
) { block(it.toLong()) }

fun boolean(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    block: ValueDefinition.(Boolean) -> Boolean = { it }
): ValueDelegate<Boolean> = value(
    name,
    default,
    lazy,
    secret
) { block(it.toBoolean()) }

fun port(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    block: ValueDefinition.(Int) -> Int = { it }
): ValueDelegate<Int> = value(name, default, lazy, secret) { value ->
    block(
        when (val port = value.toInt()) {
            0 -> ServerSocket(0).use { it.localPort }
            else -> port
        }
    )
}

fun <T : Any> Module.bind(bean: Bean<T>) = BindBean(bean)

fun <T : Any> Module.bind(value: Value<T>) = BindProperty(value)

fun <T : Any> Module.onCreate(bean: Bean<T>, block: BeanConsumerDefinition<T>.(T) -> Unit) {
    val env = bean.container.environment
    bean.onCreate { instance, consumer ->
        BeanConsumerDefinition(env, instance, consumer).block(instance)
    }
}

fun <T : Any> Module.onDestroy(bean: Bean<T>, block: BeanConsumerDefinition<T>.(T) -> Unit) {
    val env = bean.container.environment
    bean.onDestroy { instance, consumer ->
        BeanConsumerDefinition(env, instance, consumer).block(instance)
    }
}

interface BeanDelegate<out T : Any> : Delegate<Bean<T>>
interface ValueDelegate<out T : Any> : Delegate<Value<T>>
interface ModuleDelegate<out T : Module> : Delegate<T>

interface Delegate<out T> {
    operator fun provideDelegate(thisRef: Module, property: KProperty<*>): Property<T>
}

interface Property<out T> : ReadOnlyProperty<Module, T>

class BindBean<T : Any> internal constructor(private val bean: Bean<T>) {
    infix fun to(block: BeanSupplierDefinition<T>.() -> T) {
        val env = bean.container.environment
        bean.onBind { next ->
            BeanSupplierDefinition(env, next).block()
        }
    }
}

class BindProperty<T : Any> internal constructor(private val value: Value<T>) {
    infix fun to(block: ValueSupplierDefinition<T>.() -> T) {
        val env = value.container.environment
        value.onBind { next ->
            ValueSupplierDefinition(env, next).block()
        }
    }
}
