/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.Value
import io.kraftverk.binding.handler
import io.kraftverk.declaration.BeanSupplierInterceptorDeclaration
import io.kraftverk.declaration.ValueSupplierDeclaration
import io.kraftverk.internal.container.Container

/**
 * The [bind] method binds an existing bean to a new implementation.
 *
 * It is useful when doing tests, for example when mocking:
 * ```kotlin
 * val app = Kraftverk.manage { AppModule() }
 * app.start {
 *     bind(repository) to { mockk() }
 * }
 * ```
 * Or when creating a spy:
 * ```kotlin
 * val app = Kraftverk.manage { AppModule() }
 * app.start {
 *     bind(repository) to { spyk(next()) } // Use next() to get hold of the actual instance
 * }
 * ```
 * Another use case is reconfiguration of a nested module:
 * ```kotlin
 * class AppModule : Module() {
 *     val rabbit by module { RabbitModule() }
 *     init {
 *         bind(rabbit.username) to { "testuser" }
 *         bind(rabbit.connectionFactory) to {
 *             MySpecialConnectionFactory()
 *         }
 *     }
 * }
 * ```
 */

fun <T : Any> BasicModule<*>.bind(bean: Bean<T>) =
    BeanBinder(container, bean)

/**
 * Binds a [Value] to a new value, for example:
 * ```kotlin
 * val app = Kraftverk.manage { AppModule() }
 * app.start { // this: AppModule
 *     bind(rabbit.username) to { "testuser" }
 * }
 * ```
 * The provided value will override any other values.
 */
fun <T : Any> BasicModule<*>.bind(value: Value<T>) =
    ValueBinder(container, value)

/**
 * Helper class for the [Module.bind] method.
 */
class BeanBinder<T : Any> internal constructor(
    private val container: Container,
    private val bean: Bean<T>
) {
    infix fun to(block: BeanSupplierInterceptorDeclaration<T>.() -> T) {
        bean.handler.bind { proceed ->
            BeanSupplierInterceptorDeclaration(container, proceed).block()
        }
    }
}

/**
 * Helper class for the [Module.bind] method.
 */
class ValueBinder<T : Any> internal constructor(
    private val container: Container,
    private val value: Value<T>
) {
    infix fun to(block: ValueSupplierDeclaration<T>.() -> T) {
        value.handler.bind { proceed ->
            ValueSupplierDeclaration(container, proceed).block()
        }
    }
}
