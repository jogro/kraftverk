/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.Value
import io.kraftverk.binding.handler
import io.kraftverk.definition.BeanConsumerDefinition
import io.kraftverk.definition.BeanDefinition
import io.kraftverk.definition.BeanSupplierDefinition
import io.kraftverk.definition.ValueDefinition
import io.kraftverk.definition.ValueSupplierDefinition
import io.kraftverk.internal.container.Container
import io.kraftverk.internal.module.BasicModule

/**
 * A [Module] is the place where [Bean]s and [Value]s are defined.
 */
abstract class Module : BasicModule() {

    inline fun <reified T : Any> bean(
        lazy: Boolean? = null,
        noinline instance: BeanDefinition.() -> T
    ): BeanComponent<T> = newBeanComponent(
        BeanConfig(
            T::class,
            lazy,
            instance
        )
    )

    inline fun <reified T : Any> value(
        name: String? = null,
        default: String? = null,
        lazy: Boolean? = null,
        secret: Boolean = false,
        noinline instance: ValueDefinition.(Any) -> T
    ): ValueComponent<T> =
        newValueComponent(
            name,
            ValueConfig(
                T::class,
                default,
                lazy,
                secret,
                instance
            )
        )

    fun <M : Module> module(
        name: String? = null,
        module: () -> M
    ): SubModuleComponent<M> =
        newSubModuleComponent(
            name,
            module
        )

    /**
     * The [bind] method binds an existing [Bean] to a new implementation.
     *
     * It is useful when doing tests, for example when mocking:
     * ```kotlin
     * val app = Kraftverk.manage { AppModule() }
     * app.start { // this: AppModule
     *     bind(repository) to { mockk() }
     * }
     * ```
     * Or when creating a spy:
     * ```kotlin
     * val app = Kraftverk.manage { AppModule() }
     * app.start { // this: AppModule
     *     bind(repository) to { spyk(next()) } // Use next() to get hold of the actual instance
     * }
     * ```
     * Another use case is reconfiguration of a sub module:
     * ```kotlin
     * class AppModule : Module() {
     *     val rabbit by module { RabbitModule() }
     *     init {
     *         bind(rabbit.connectionFactory) to {
     *             MySpecialConnectionFactory()
     *         }
     *     }
     * }
     * ```
     */
    fun <T : Any> bind(bean: Bean<T>) = BeanBinder(container, bean)

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
    fun <T : Any> bind(value: Value<T>) = ValueBinder(container, value)

    /**
     * A lifecycle method that makes it possible to perform som operations
     * when a [bean] instance is created, for example:
     * ```kotlin
     * class AppModule : Module() {
     *     val server by bean { Server() }
     *     init {
     *         onCreate(server) { // this: BeanConsumerDefinition
     *             it.start()
     *         }
     *         [...]
     *     }
     * }
     * ```
     */
    fun <T : Any> onCreate(
        bean: Bean<T>,
        block: BeanConsumerDefinition<T>.(T) -> Unit
    ) {
        bean.handler.onCreate { instance, nextOnCreate ->
            val definition = BeanConsumerDefinition(
                container.environment,
                instance,
                nextOnCreate
            )
            definition.block(instance)
        }
    }

    /**
     * A lifecycle method that makes it possible to perform som operations
     * when a [bean] instance is destroyed, for example:
     * ```kotlin
     * class AppModule : Module() {
     *     val server by bean { Server() }
     *     init {
     *         [...]
     *         onDestroy(server) { // this: BeanConsumerDefinition
     *             it.stop()
     *         }
     *     }
     * }
     * ```
     */
    fun <T : Any> onDestroy(
        bean: Bean<T>,
        block: BeanConsumerDefinition<T>.(T) -> Unit
    ) {
        bean.handler.onDestroy { instance, nextOnDestroy ->
            val definition = BeanConsumerDefinition(
                container.environment,
                instance,
                nextOnDestroy
            )
            definition.block(instance)
        }
    }
}

/**
 * Helper class for the [Module.bind] method.
 */
class BeanBinder<T : Any> internal constructor(
    private val container: Container,
    private val bean: Bean<T>
) {
    infix fun to(block: BeanSupplierDefinition<T>.() -> T) {
        bean.handler.onBind { nextBind ->
            BeanSupplierDefinition(container.environment, nextBind).block()
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
    infix fun to(block: ValueSupplierDefinition<T>.() -> T) {
        value.handler.onBind { nextBind ->
            ValueSupplierDefinition(container.environment, nextBind).block()
        }
    }
}
