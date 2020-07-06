/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.core.module

import io.kraftverk.core.binding.Bean
import io.kraftverk.core.binding.BeanImpl
import io.kraftverk.core.common.BeanDefinition
import io.kraftverk.core.declaration.BeanDeclaration
import io.kraftverk.core.declaration.BeanDeclarationContext
import io.kraftverk.core.internal.container.createBean
import io.kraftverk.core.internal.container.createBeanInstance
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * The bean method declares a [Bean][io.kraftverk.core.binding.Bean] within a Kraftverk module.
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
 * A Bean can be used to inject other beans:
 *
 * ```kotlin
 * class AppModule : Module() {
 *     val dataSource by bean { HikariDataSource() }
 *     val repository by bean { Repository(dataSource()) } // <--- Injection
 * }
 * ```
 *
 * Note that injection occurs by syntactically invoking the Bean as a function (operator invoke). Also note that
 * injection only is available in the context of a BeanDeclaration[io.kraftverk.core.declaration.BeanDeclaration]
 * that is provided by the bean[io.kraftverk.core.module.bean] function.
 *
 * ```kotlin
 * class AppModule : Module() {
 *     val dataSource by bean { this: BeanDeclaration
 *         [...]
 *     }
 * }
 * ```
 *
 * An important feature is the ability to rebind a Bean after it has been declared but the module still
 * hasn't been started[io.kraftverk.core.managed.start]. This feature provides the foundation for mocking etc,
 * see bind[io.kraftverk.core.module.bind].
 */
inline fun <reified T : Any> BasicModule<*>.bean(
    lazy: Boolean? = null,
    noinline instance: BeanDeclaration.() -> T
): BeanDelegateProvider<T> = bean(T::class, lazy, instance)

@PublishedApi
internal fun <T : Any> BasicModule<*>.bean(
    type: KClass<T>,
    lazy: Boolean? = null,
    instance: BeanDeclaration.() -> T
): BeanDelegateProvider<T> = object :
    BeanDelegateProvider<T> {
    override fun provideDelegate(
        thisRef: BasicModule<*>,
        property: KProperty<*>
    ): ReadOnlyProperty<BasicModule<*>, Bean<T>> {
        val qualifiedName = qualifyName(property.name)
        return createBean(qualifiedName, type, lazy, instance).let(::Delegate)
    }
}

private fun <T : Any> BasicModule<*>.createBean(
    name: String,
    type: KClass<T>,
    lazy: Boolean? = null,
    instance: BeanDeclaration.() -> T
): BeanImpl<T> {
    val ctx = BeanDeclarationContext()
    val definition = BeanDefinition(
        name = name,
        lazy = lazy,
        type = type,
        instance = { container.createBeanInstance(ctx, instance) }
    )
    return container.createBean(definition, ctx)
}
