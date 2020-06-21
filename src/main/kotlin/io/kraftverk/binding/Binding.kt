/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.binding

import io.kraftverk.internal.binding.BindingHandler
import io.kraftverk.internal.binding.BindingProviderFactory
import io.kraftverk.internal.binding.ComponentHandler
import io.kraftverk.internal.binding.ValueHandler
import io.kraftverk.internal.binding.provider
import io.kraftverk.provider.ComponentProvider
import io.kraftverk.provider.Provider
import io.kraftverk.provider.ValueProvider

/**
 * A Binding is a [CustomBean] or [Value] that is declared within a Kraftverk managed module.
 *
 * Common for all Bindings is that they serve as factories that produce injectable singleton instances
 * of type [T].
 *
 * A Binding can be obtained by calling for example the component[io.kraftverk.module.bean] declaration function:
 *
 * '''Kotlin
 * class AppModule : Module() {
 *     val dataSource by component { HikariDataSource() }  // Results in a Component<HikariDataSource>
 * }
 * '''
 *
 * Binding is covariant so this is also a valid declaration:
 *
 * '''Kotlin
 * class AppModule : Module() {
 *     val dataSource: Binding<HikariDataSource> by component { HikariDataSource() }
 * }
 *
 */
sealed class Binding<out T : Any>

/**
 * A Component is a specialized [Binding] that can be declared within a module[io.kraftverk.module.Module]
 * managed by Kraftverk.
 *
 * The primary purpose of a Component is to serve as a configurable factory that produces injectable singleton
 * instances of type [T].
 *
 * A Component is obtained by calling the component[io.kraftverk.module.bean] declaration function like this:
 *
 * '''Kotlin
 * class AppModule : Module() {
 *     val dataSource by component { HikariDataSource() }  // Component<HikariDataSource>
 * }
 * '''
 *
 * Or this:
 *
 * '''Kotlin
 * class AppModule : Module() {
 *     val dataSource by component<DataSource> { HikariDataSource() } // Component<DataSource>
 * }
 *
 * A Component can be used to inject other components:
 *
'''Kotlin
 * class AppModule : Module() {
 *     val dataSource by component { HikariDataSource() }
 *     val repository by component { Repository(dataSource()) } // <--- Injection of the data source
 * }
 * '''
 *
 * Note that injection occurs by syntactically invoking the Component as a function (operator invoke). Also note that
 * injection only is available in the context of a ComponentDeclaration[io.kraftverk.declaration.ComponentDeclaration]
 * that is provided by the component[io.kraftverk.module.bean] function.
 *
 * '''Kotlin
 * class AppModule : Module() {
 *     val dataSource by component { this: ComponentDeclaration
 *         [...]
 *     }
 * }
 * '''
 *
 * An important feature is the ability to rebind a Component after it has been declared but the module still
 * hasn't been started[io.kraftverk.managed.start]. This feature provides the foundation for mocking etc,
 * see bind[io.kraftverk.module.bind].
 *
 * Components can also be lifecycle handled by use of the onCreate[io.kraftverk.module.onCreate] and
 * onDestroy[io.kraftverk.module.onDestroy] functions provided by the module[io.kraftverk.module.Module].
 *
 * In Kraftverk there is no need to support a binding scope like 'prototype' etc since this
 * can be achieved in other ways:
 *
 * '''Kotlin
 * class AppModule : Module() {
 *     val user by component { { User() } }
 * }
 * '''
 * Here the binding is inferred to be Component<() -> User> and can be injected like this:
 *
 * '''Kotlin
 * class AppModule : Module() {
 *     val user by component { { User() } }
 *     val session by component { Session(user()()) }
 * }
 * '''
 *
 */

sealed class Component<out T : Any> : Binding<T>()

sealed class CustomBean<out T : Any, out S : Any> : Component<T>() {
    companion object
}

sealed class Bean<out T : Any> : Component<T>() {
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
 *     val dataSource by component { MyDataSource(poolSize()) } // <--- Injection of the pool size
 * }
 * '''
 *
 * Note that injection occurs by syntactically invoking the Value as a function (operator invoke). Also note that
 * injection only is available in the context of a ComponentDeclaration[io.kraftverk.declaration.ComponentDeclaration]
 * or a ValueDeclaration[io.kraftverk.declaration.ValueDeclaration].
 *
 * '''Kotlin
 * class AppModule : Module() {
 *     val dataSource by component { this: ComponentDeclaration
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
sealed class Value<out T : Any> : Binding<T>() {
    companion object
}

internal class CustomBeanImpl<T : Any, S : Any>(val handler: ComponentHandler<T, S>) : CustomBean<T, S>()

internal class BeanImpl<T : Any>(val handler: ComponentHandler<T, T>) : Bean<T>()

internal class ValueImpl<T : Any>(val handler: ValueHandler<T>) : Value<T>()

internal val <T : Any, S : Any> CustomBean<T, S>.handler: ComponentHandler<T, S>
    get() = when (this) {
        is CustomBeanImpl<T, S> -> handler
    }

internal val <T : Any> Bean<T>.handler: ComponentHandler<T, T>
    get() = when (this) {
        is BeanImpl<T> -> handler
    }

internal val <T : Any> Value<T>.handler: ValueHandler<T>
    get() = when (this) {
        is ValueImpl<T> -> handler
    }

internal val <T : Any> Binding<T>.handler: BindingHandler<T, Provider<T>, BindingProviderFactory<T, Provider<T>>>
    get() = when (this) {
        is ValueImpl<T> -> handler
        is CustomBeanImpl<T, *> -> handler
        is BeanImpl<T> -> handler
    }

internal val <T : Any> Value<T>.provider: ValueProvider<T> get() = handler.provider
internal val <T : Any, S : Any> CustomBean<T, S>.provider: ComponentProvider<T, S> get() = handler.provider
internal val <T : Any> Bean<T>.provider: ComponentProvider<T, T> get() = handler.provider
