/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.binding

import io.kraftverk.internal.binding.BindingHandler
import io.kraftverk.internal.binding.ComponentHandler
import io.kraftverk.internal.binding.ValueHandler
import io.kraftverk.internal.binding.provider
import io.kraftverk.provider.ComponentProvider
import io.kraftverk.provider.Provider
import io.kraftverk.provider.ValueProvider

/**
 * A Binding is a [Component] or [Value] that is declared within a Kraftverk managed module.
 *
 * Common for all Bindings is that they serve as factories that produce injectable singleton instances
 * of type [T].
 *
 * A Binding can be obtained by calling for example the bean[io.kraftverk.module.component] declaration function:
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
sealed class Binding<out T : Any, out S : Any>

/**
 * A Bean is a specialized [Binding] that can be declared within a module[io.kraftverk.module.Module]
 * managed by Kraftverk.
 *
 * The primary purpose of a Bean is to serve as a configurable factory that produces injectable singleton
 * instances of type [T].
 *
 * A Bean is obtained by calling the bean[io.kraftverk.module.component] declaration function like this:
 *
 * '''Kotlin
 * class AppModule : Module() {
 *     val dataSource by bean { HikariDataSource() }  // Bean<HikariDataSource>
 * }
 * '''
 *
 * Or this:
 *
 * '''Kotlin
 * class AppModule : Module() {
 *     val dataSource by bean<DataSource> { HikariDataSource() } // Bean<DataSource>
 * }
 *
 * A Bean can be used to inject other beans:
 *
'''Kotlin
 * class AppModule : Module() {
 *     val dataSource by bean { HikariDataSource() }
 *     val repository by bean { Repository(dataSource()) } // <--- Injection of the data source
 * }
 * '''
 *
 * Note that injection occurs by syntactically invoking the Bean as a function (operator invoke). Also note that
 * injection only is available in the context of a BeanDeclaration[io.kraftverk.declaration.ComponentDeclaration]
 * that is provided by the bean[io.kraftverk.module.component] function.
 *
 * '''Kotlin
 * class AppModule : Module() {
 *     val dataSource by bean { this: BeanDeclaration
 *         [...]
 *     }
 * }
 * '''
 *
 * An important feature is the ability to rebind a Bean after it has been declared but the module still
 * hasn't been started[io.kraftverk.managed.start]. This feature provides the foundation for mocking etc,
 * see bind[io.kraftverk.module.bind].
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
 *     val user by bean { { User() } }
 *     val session by bean { Session(user()()) }
 * }
 * '''
 *
 */
sealed class Component<out T : Any, out S : Any> : Binding<T, S>() {
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
 *     val uri by value { v -> URI(v.toString()) } // Value<URI>
 * }
 * '''
 *
 * But there exist also more specialized standard value declaration functions:
 *
 * '''Kotlin
 * class JdbcModule : Module() {
 *     val username by string() // Value<String>
 *     val poolSize by int() // Value<Int>
 *     [...]
 * }
 * '''
 * See string[io.kraftverk.module.string], int[io.kraftverk.module.int], long[io.kraftverk.module.long],
 * boolean[io.kraftverk.module.boolean] and port[io.kraftverk.module.port].
 *
 * Values are injectable:
 *
 * '''Kotlin
 * class JdbcModule : Module() {
 *     val poolSize by int()
 *     val dataSource by bean { MyDataSource(poolSize()) } // <--- Injection of the pool size
 * }
 * '''
 *
 * Note that injection occurs by syntactically invoking the Value as a function (operator invoke). Also note that
 * injection only is available in the context of a BeanDeclaration[io.kraftverk.declaration.ComponentDeclaration]
 * or a ValueDeclaration[io.kraftverk.declaration.ValueDeclaration].
 *
 * '''Kotlin
 * class AppModule : Module() {
 *     val dataSource by bean { this: BeanDeclaration
 *         [...]
 *     }
 * }
 * '''
 *
 * An important feature is the ability to rebind a Value after it has been declared but the module still
 * hasn't been started[io.kraftverk.managed.start]. This feature provides the foundation for mocking etc,
 * see bind[io.kraftverk.module.bind].
 *
 */
sealed class Value<out T : Any, S : Any> : Binding<T, S>() {
    companion object
}

internal class ComponentImpl<T : Any, S : Any>(val handler: ComponentHandler<T, S>) : Component<T, S>()

internal class ValueImpl<T : Any, S : Any>(val handler: ValueHandler<T, S>) : Value<T, S>()

internal val <T : Any, S : Any> Component<T, S>.handler: ComponentHandler<T, S>
    get() = when (this) {
        is ComponentImpl<T, S> -> handler
    }

internal val <T : Any, S : Any> Value<T, S>.handler: ValueHandler<T, S>
    get() = when (this) {
        is ValueImpl<T, S> -> handler
    }

internal val <T : Any, S : Any> Binding<T, S>.handler: BindingHandler<T, S, Provider<T>>
    get() = when (this) {
        is ComponentImpl<T, S> -> handler
        is ValueImpl<T, S> -> handler
    }

internal val <T : Any, S : Any> Component<T, S>.provider: ComponentProvider<T, S> get() = handler.provider
internal val <T : Any, S : Any> Value<T, S>.provider: ValueProvider<T> get() = handler.provider
internal val <T : Any, S : Any> Binding<T, S>.provider: Provider<T> get() = handler.provider
