/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.*
import java.net.ServerSocket

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

fun <T : Any> Module.bind(bean: Bean<T>) = BindBean(container, bean)

fun <T : Any> Module.bind(value: Value<T>) = BindValue(container, value)

fun <T : Any> Module.onCreate(
    bean: Bean<T>,
    block: BeanConsumerDefinition<T>.(T) -> Unit
) {
    bean.onCreate { instance, consumer ->
        BeanConsumerDefinition(
            container.environment,
            instance,
            consumer
        ).block(instance)
    }
}

fun <T : Any> Module.onDestroy(
    bean: Bean<T>,
    block: BeanConsumerDefinition<T>.(T) -> Unit
) {
    bean.onDestroy { instance, consumer ->
        BeanConsumerDefinition(
            container.environment,
            instance,
            consumer
        ).block(instance)
    }
}

class BindBean<T : Any> internal constructor(
    private val container: Container,
    private val bean: Bean<T>
) {
    infix fun to(block: BeanSupplierDefinition<T>.() -> T) {
        bean.onBind { next ->
            BeanSupplierDefinition(container.environment, next).block()
        }
    }
}

class BindValue<T : Any> internal constructor(
    private val container: Container,
    private val value: Value<T>
) {
    infix fun to(block: ValueSupplierDefinition<T>.() -> T) {
        value.onBind { next ->
            ValueSupplierDefinition(container.environment, next).block()
        }
    }
}
