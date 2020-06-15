/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Component
import io.kraftverk.binding.Value
import io.kraftverk.binding.XBean
import io.kraftverk.binding.handler
import io.kraftverk.declaration.ComponentSupplierInterceptorDeclaration
import io.kraftverk.declaration.ValueSupplierDeclaration
import io.kraftverk.internal.binding.bind
import io.kraftverk.internal.container.Container

/**
 * The [bind] method binds an existing [XBean] to a new implementation.
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

fun <T : Any> AbstractModule.bind(component: Component<T>) =
    ComponentBinder(container, component)

/**
 * Binds a configured [Value] to a new value, for example:
 * ```kotlin
 * val app = Kraftverk.manage { AppModule() }
 * app.start { // this: AppModule
 *     bind(rabbit.username) to { "testuser" }
 * }
 * ```
 * The provided value will override any other values.
 */
fun <T : Any> AbstractModule.bind(value: Value<T>) =
    ValueBinder(container, value)

/**
 * Helper class for the [Module.bind] method.
 */
class ComponentBinder<T : Any> internal constructor(
    private val container: Container,
    private val component: Component<T>
) {
    infix fun to(block: ComponentSupplierInterceptorDeclaration<T>.() -> T) {
        component.handler.bind { proceed ->
            ComponentSupplierInterceptorDeclaration(container, proceed).block()
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
