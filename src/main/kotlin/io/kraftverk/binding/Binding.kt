/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.binding

import io.kraftverk.internal.binding.BeanHandler
import io.kraftverk.internal.binding.BindingHandler
import io.kraftverk.internal.binding.ValueHandler
import io.kraftverk.internal.binding.provider
import io.kraftverk.provider.BeanProvider
import io.kraftverk.provider.Provider
import io.kraftverk.provider.ValueProvider

/**
 * A Binding is a [Bean] or [Value] that is declared within a module[io.kraftverk.module.Module]
 * that is managed by Kraftverk.
 *
 * A Binding is obtained by use of the bean[io.kraftverk.module.bean] and value[io.kraftverk.module.value]
 * binding declaration functions like for example this:
 *
 * '''Kotlin
 * class AppModule : Module() {
 *     val dataSource by bean { HikariDataSource() }  // Results in a Bean<HikariDataSource>
 * }
 * '''
 *
 * Binding is covariant so this is also a valid declaration:
 *
 * '''Kotlin
 * class AppModule : Module() {
 *     val dataSource: Binding<HikariDataSource> by bean { HikariDataSource() }
 * }
 *
 */
sealed class Binding<out T : Any>

/**
 * A Bean is a specialized [Binding] that can be declared within a module[io.kraftverk.module.Module]
 * managed by Kraftverk.
 *
 * The primary purpose of a Bean is to serve as a configurable factory that produces injectable singleton
 * instances of type [T].
 *
 * A Bean is obtained by calling the bean[io.kraftverk.module.bean] declaration function like this:
 *
 * '''Kotlin
 * class AppModule : Module() {
 *     val dataSource by bean { HikariDataSource() }  // Results in a Bean<HikariDataSource>
 * }
 * '''
 *
 * Or this:
 *
 * '''Kotlin
 * class AppModule : Module() {
 *     val dataSource by bean<DataSource> { HikariDataSource() } // Results in a Bean<DataSource>
 * }
 *
 * A Bean can be used to inject other beans:
 *
'''Kotlin
 * class AppModule : Module() {
 *     val repository by bean { Repository(dataSource()) } // <--- Injection of the data source
 *     val dataSource by bean { HikariDataSource() }
 * }
 * '''
 *
 * Note that injection occurs by syntactically invoking the Bean as a function (operator invoke). Also note that
 * injection only is available in the context of a BeanDeclaration[io.kraftverk.declaration.BeanDeclaration]
 * that is provided by the bean[io.kraftverk.module.bean] function.
 *
 * '''Kotlin
 * class AppModule : Module() {
 *     val dataSource by bean { this: BeanDeclaration
 *         [...]
 *     }
 * }
 * '''
 *
 * An important feature is the ability to rebind a Bean after it has been declared but still hasn't been
 * started. This feature provides the foundation for mocking etc, see bind[io.kraftverk.module.bind].
 *
 * Beans can also be lifecycle handled by use of the onCreate[io.kraftverk.module.onCreate] and
 * onDestroy[io.kraftverk.module.onDestroy] functions provided by the module[io.kraftverk.module.Module].
 *
 * In Kraftverk there is no need to support a binding scope like 'prototype' etc since this
 * can be achieved in other ways:
 *
 * '''Kotlin
 * class AppModule : Module() {
 *     val user by bean { { User() } }
 * }
 * '''
 * Here the binding is inferred to be Bean<() -> User> and can be injected like this:
 *
 * '''Kotlin
 * class AppModule : Module() {
 *     val session by bean { Session(user()()) }
 *     val user by bean { { User() } }
 * }
 * '''
 */
sealed class Bean<out T : Any> : Binding<T>() {
    companion object
}

/**
 * A Value is a specialized [Binding] that can be declared within a module[io.kraftverk.module.Module]
 * managed by Kraftverk.
 *
 * The primary purpose of a Value is to provide easy access to properties, environment variables etc that have
 * been defined in the Environment[io.kraftverk.env.Environment].
 *
 * There exist several functions to help declaring a Value.
 *
 * The most basic one is value[io.kraftverk.module.value] that can be used like this:
 *
 * '''Kotlin
 * class AppModule : Module() {
 *     val uri by value { v -> URI(v.toString()) } // Results in a Value<URI>
 * }
 * '''
 *
 * But there exist also more specialized standard value declaration functions:
 *
 * '''Kotlin
 * class JdbcModule : Module() {
 *     val username by string() // Results in a Value<String>
 *     val poolSize by int() // Results in a Value<Int>
 *     [...]
 * }
 * '''
 *
 * See string[io.kraftverk.module.string], int[io.kraftverk.module.int], long[io.kraftverk.module.long],
 * boolean[io.kraftverk.module.boolean] and port[io.kraftverk.module.port].
 *
 * An important feature is the ability to rebind a Value after it has been declared but the module is not
 * yet started which provides the foundation for mocking etc, see bind[io.kraftverk.module.bind].
 */
sealed class Value<out T : Any> : Binding<T>() {
    companion object
}

internal class BeanImpl<T : Any>(val handler: BeanHandler<T>) : Bean<T>()
internal class ValueImpl<T : Any>(val handler: ValueHandler<T>) : Value<T>()

internal val <T : Any> Bean<T>.handler: BeanHandler<T>
    get() = when (this) {
        is BeanImpl<T> -> handler
    }

internal val <T : Any> Value<T>.handler: ValueHandler<T>
    get() = when (this) {
        is ValueImpl<T> -> handler
    }

internal val <T : Any> Binding<T>.handler: BindingHandler<T, Provider<T>>
    get() = when (this) {
        is BeanImpl<T> -> handler
        is ValueImpl<T> -> handler
    }

internal val <T : Any> Bean<T>.provider: BeanProvider<T> get() = handler.provider
internal val <T : Any> Value<T>.provider: ValueProvider<T> get() = handler.provider
internal val <T : Any> Binding<T>.provider: Provider<T> get() = handler.provider
