/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.*
import java.net.ServerSocket
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Basic component to inherit from when creating a module.
 */
abstract class Module {

    internal val containerContext: ContainerContext = threadBoundContainerContext.get()
    internal val namespace: String = threadBoundNamespace.get()

    inner class BindBean<T : Any>(private val bean: Bean<T>) {
        infix fun to(block: BeanSupplierDefinition<T>.() -> T) {
            bean.onBind { next ->
                BeanSupplierDefinition(containerContext.profiles, next).block()
            }
        }
    }

    inner class BindProperty<T : Any>(private val property: Property<T>) {
        infix fun to(block: PropertySupplierDefinition<T>.() -> T) {
            property.onBind { next ->
                PropertySupplierDefinition(containerContext.profiles, next).block()
            }
        }
    }

    companion object {
        internal val threadBoundContainerContext = ThreadBound<ContainerContext>()
        internal val threadBoundNamespace = ThreadBound<String>()
    }

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

fun Module.intProperty(
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

fun Module.longProperty(
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

fun Module.booleanProperty(
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

fun Module.portProperty(
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

inline fun <reified T : Any> Module.property(
    name: String? = null,
    default: String? = null,
    lazy: Boolean? = null,
    secret: Boolean = false,
    noinline instance: PropertyDefinition.(String) -> T
): DelegateProvider<Module, Property<T>> =
    newProperty(
        T::class,
        name,
        default,
        lazy,
        secret,
        instance
    )

fun <M : Module> Module.Companion.module(
    name: String? = null,
    module: () -> M
): DelegateProvider<Module, M> =
    object :
        DelegateProvider<Module, M> {
        override fun provideDelegate(thisRef: Module, prop: KProperty<*>): ReadOnlyProperty<Module, M> {
            val namespace = name ?: prop.name
            val subModule = if (namespace.isEmpty()) module() else {
                val currentNamespace = threadBoundNamespace.get()
                val newNamespace = if (currentNamespace.isEmpty()) namespace else "$currentNamespace.$namespace"
                use(newNamespace) {
                    module()
                }
            }
            return object : ReadOnlyProperty<Module, M> {
                override fun getValue(thisRef: Module, property: KProperty<*>): M {
                    return subModule
                }
            }
        }
    }

fun <T : Any> Module.bind(bean: Bean<T>) = BindBean(bean)

fun <T : Any> Module.bind(property: Property<T>) = BindProperty(property)

fun <T : Any> Module.onCreate(bean: Bean<T>, block: BeanConsumerDefinition<T>.(T) -> Unit) {
    bean.onCreate { instance, consumer ->
        BeanConsumerDefinition(containerContext.profiles, instance, consumer).block(instance)
    }
}

fun <T : Any> Module.onDestroy(bean: Bean<T>, block: BeanConsumerDefinition<T>.(T) -> Unit) {
    bean.onDestroy { instance, consumer ->
        BeanConsumerDefinition(containerContext.profiles, instance, consumer).block(instance)
    }
}

@PublishedApi
internal fun <T : Any> Module.newBean(
    type: KClass<T>,
    lazy: Boolean? = null,
    instance: BeanDefinition.() -> T
): DelegateProvider<Module, Bean<T>> =
    containerContext.newBean(
        type,
        lazy,
        namespace,
        instance
    )

@PublishedApi
internal fun <T : Any> Module.newProperty(
    type: KClass<T>,
    name: String?,
    default: String?,
    lazy: Boolean?,
    secret: Boolean,
    instance: PropertyDefinition.(String) -> T
): DelegateProvider<Module, Property<T>> =
    containerContext.newProperty(
        type,
        name,
        default,
        lazy,
        secret,
        namespace,
        instance
    )

internal fun <M : Module> Module.Companion.create(containerContext: ContainerContext, namespace: String, moduleFun: () -> M): M {
    contract {
        callsInPlace(moduleFun, InvocationKind.EXACTLY_ONCE)
    }
    return use(containerContext) {
        use(namespace) {
            moduleFun()
        }
    }
}

internal fun <R> Module.Companion.use(namespace: String, block: () -> R): R {
    return threadBoundNamespace.use(namespace, block)
}

private fun <R> Module.Companion.use(containerContext: ContainerContext, block: () -> R): R {
    return threadBoundContainerContext.use(containerContext, block)
}

interface DelegateProvider<in R, out T> {
    operator fun provideDelegate(thisRef: R, prop: KProperty<*>): ReadOnlyProperty<R, T>
}

internal class ThreadBound<T> {

    private val threadLocal = ThreadLocal<T>()

    fun get(): T = threadLocal.get() ?: throw IllegalStateException()

    fun <R> use(value: T, block: () -> R): R {
        val previous: T? = threadLocal.get()
        threadLocal.set(value)
        try {
            return block()
        } finally {
            if (previous == null) {
                threadLocal.remove()
            } else {
                threadLocal.set(previous)
            }
        }
    }

}