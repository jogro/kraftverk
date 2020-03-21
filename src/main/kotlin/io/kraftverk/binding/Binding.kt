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
 * The primary purpose of a Binding is to serve as a configurable factory that produces injectable
 * singleton instances of type [T].
 *
 * A Binding is obtained by use of the bean[io.kraftverk.module.bean] and value[io.kraftverk.module.value]
 * binding declaration functions like this:
 *
 * '''Kotlin
 * class AppModule : Module() {
 *     val dataSource by bean { HikariDataSource() }  // Results in a Bean<HikariDataSource>
 * }
 * '''
 *
 * Or this:
 * '''Kotlin
 * class AppModule : Module() {
 *     val dataSource by bean<DataSource> { HikariDataSource() } // Results in a Bean<DataSource>
 * }
 *
 * Binding is covariant so this is also a valid declaration:
 * '''Kotlin
 * class AppModule : Module() {
 *     val dataSource: Binding<HikariDataSource> by bean { HikariDataSource() }
 * }
 *
 * As mentioned above the Binding is primarily a factory of singleton instances of type [T]. An
 * important concept is that this singleton instance [T] can *only* be obtained, i.e. injected,
 * within the context of a BeanDefinition[io.kraftverk.definition.BeanDefinition] or
 * ValueDefinition[io.kraftverk.definition.ValueDefinition]. This context is provided by the
 * bean[io.kraftverk.module.bean] and value[io.kraftverk.module.value] declaration functions
 * like this:
 *
 * '''Kotlin
 * class AppModule : Module() {
 *     val dataSource by bean { this: BeanDefinition
 *         [...]
 *     }
 * }
 * '''
 * The injection mechanism i implemented as an operator invoke extension function that is available
 * only in this context:
 *
'''Kotlin
 * class AppModule : Module() {
 *     val repository by bean { Repository(dataSource()) } // <--- Injection of the data source
 *     val dataSource by bean { HikariDataSource() }
 * }
 * '''
 *
 * In Kraftverk there is no need to support special binding constructs like 'Spring Prototypes' since this
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
 *     val session by bean { { Session(user()()) } }
 *     val user by bean { { User() } }
 * }
 * '''
 */
sealed class Binding<out T : Any>

sealed class Bean<out T : Any> : Binding<T>() {
    companion object
}

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
