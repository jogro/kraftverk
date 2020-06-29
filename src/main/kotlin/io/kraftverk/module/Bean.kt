/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.BeanImpl
import io.kraftverk.common.ComponentDefinition
import io.kraftverk.declaration.ComponentDeclaration
import io.kraftverk.internal.container.createBean
import io.kraftverk.internal.container.createComponentInstance
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * The bean method declares a Bean [io.kraftverk.binding.Bean] within a Kraftverk module.
 *
 * Like this:
 *
 * ```kotlin
 * class AppModule : Module() {
 *     val dataSource by bean { HikariDataSource() }  // Bean<HikariDataSource>
 * }
 * ```
 *
 * Or this:
 *
 * ```kotlin
 * class AppModule : Module() {
 *     val dataSource by bean<DataSource> { HikariDataSource() } // Bean<DataSource>
 * }
 *```
 * A Bean can be used to inject other components:
 *
 * ```kotlin
 * class AppModule : Module() {
 *     val dataSource by bean { HikariDataSource() }
 *     val repository by bean { Repository(dataSource()) } // <--- Injection
 * }
 * ```
 *
 * Note that injection occurs by syntactically invoking the Component as a function (operator invoke). Also note that
 * injection only is available in the context of a ComponentDeclaration[io.kraftverk.declaration.ComponentDeclaration]
 * that is provided by the component[io.kraftverk.module.bean] function.
 *
 * ```kotlin
 * class AppModule : Module() {
 *     val dataSource by component { this: ComponentDeclaration
 *         [...]
 *     }
 * }
 * ```
 *
 * An important feature is the ability to rebind a Component after it has been declared but the module still
 * hasn't been started[io.kraftverk.managed.start]. This feature provides the foundation for mocking etc,
 * see bind[io.kraftverk.module.bind].
 */
inline fun <reified T : Any> BasicModule<*>.bean(
    lazy: Boolean? = null,
    noinline instance: ComponentDeclaration.() -> T
): BeanDelegateProvider<T> = bean(T::class, lazy, { i, setUp -> setUp(i) }, instance)

@PublishedApi
internal fun <T : Any> BasicModule<*>.bean(
    type: KClass<T>,
    lazy: Boolean? = null,
    onSetUp: (T, (T) -> Unit) -> Unit,
    instance: ComponentDeclaration.() -> T

): BeanDelegateProvider<T> = object : BeanDelegateProvider<T> {
    override fun provideDelegate(
        thisRef: BasicModule<*>,
        property: KProperty<*>
    ): ReadOnlyProperty<BasicModule<*>, Bean<T>> {
        val qualifiedName = qualifyName(property.name)
        return createBean(qualifiedName, type, lazy, onSetUp, instance).let(::Delegate)
    }
}

private fun <T : Any> BasicModule<*>.createBean(
    name: String,
    type: KClass<T>,
    lazy: Boolean? = null,
    onSetUp: (T, (T) -> Unit) -> Unit,
    instance: ComponentDeclaration.() -> T
): BeanImpl<T> {
    val config = ComponentDefinition(
        name = name,
        lazy = lazy,
        onSetUp = onSetUp,
        type = type,
        instance = { container.createComponentInstance(instance) }
    )
    return container.createBean(config)
}
