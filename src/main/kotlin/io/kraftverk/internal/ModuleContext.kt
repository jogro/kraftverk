/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

internal class ModuleContext {

    val appContext: AppContext = ModuleContext.appContext
    private val namespace: String = ModuleContext.namespace
    private val beanFactory = BeanFactory(appContext, namespace)
    private val propertyFactory = PropertyFactory(appContext, namespace)

    internal fun <T : Any> newBean(
        type: KClass<T>,
        lazy: Boolean? = null,
        define: BeanDefinition.() -> T
    ): DelegateProvider<Module, Bean<T>> =
        beanFactory.newBean(
            type,
            lazy,
            define
        )

    fun <T : Any> newProperty(
        type: KClass<T>,
        name: String? = null,
        defaultValue: String?,
        lazy: Boolean?,
        secret: Boolean,
        define: PropertyDefinition.(String) -> T
    ): DelegateProvider<Module, Property<T>> =
        propertyFactory.newProperty(
            type,
            name,
            defaultValue,
            lazy,
            secret,
            define
        )

    fun <M : Module> module(
        name: String? = null,
        module: () -> M,
        configure: M.() -> Unit
    ): DelegateProvider<Module, M> = object :
        DelegateProvider<Module, M> {
        override fun provideDelegate(thisRef: Module, prop: KProperty<*>): ReadOnlyProperty<Module, M> {
            val namespace = name ?: prop.name
            val subModule = if (namespace.isEmpty()) module() else {
                val currentNamespace = ModuleContext.namespace
                val newNamespace = if (currentNamespace.isEmpty()) namespace else "$currentNamespace.$namespace"
                use(newNamespace) {
                    module()
                }
            }.apply(configure)
            return object : ReadOnlyProperty<Module, M> {
                override fun getValue(thisRef: Module, property: KProperty<*>): M {
                    return subModule
                }
            }
        }
    }

    companion object {
        internal val contextualAppContext = Contextual<AppContext>()
        internal val contextualNamespace = Contextual<String>()
        val appContext get() = contextualAppContext.get()
        val namespace get() = contextualNamespace.get()
    }

}

internal fun <R> ModuleContext.Companion.use(appContext: AppContext, namespace: String, block: () -> R): R {
    return use(appContext) {
        use(namespace) {
            block()
        }
    }
}

internal fun <R> ModuleContext.Companion.use(namespace: String, block: () -> R): R {
    return contextualNamespace.use(namespace, block)
}

private fun <R> ModuleContext.Companion.use(appContext: AppContext, block: () -> R): R {
    return contextualAppContext.use(appContext, block)
}
