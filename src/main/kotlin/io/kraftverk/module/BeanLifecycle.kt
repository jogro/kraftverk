/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.handler
import io.kraftverk.definition.BeanConsumerDefinition
import io.kraftverk.definition.BeanConsumerInterceptorDefinition
import io.kraftverk.internal.binding.onCreate
import io.kraftverk.internal.binding.onCustomize
import io.kraftverk.internal.binding.onDestroy

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
fun <T : Any> Modular.onCreate(
    bean: Bean<T>,
    block: BeanConsumerInterceptorDefinition<T>.(T) -> Unit
) {
    bean.handler.onCreate { instance, proceed ->
        val definition = BeanConsumerInterceptorDefinition(
            container,
            instance,
            proceed
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
fun <T : Any> Modular.onDestroy(
    bean: Bean<T>,
    block: BeanConsumerInterceptorDefinition<T>.(T) -> Unit
) {
    bean.handler.onDestroy { instance, proceed ->
        val definition = BeanConsumerInterceptorDefinition(
            container,
            instance,
            proceed
        )
        definition.block(instance)
    }
}

/**
 * A helper method for customization of a bean after it has been declared, for example:
 * ```kotlin
 * class JdbcModule : Module() {
 *     [...]
 *     val config by bean { HikariConfig() }
 *     val dataSource by bean { HikariDataSource(config()) }
 *     init {
 *         customize(config) { c ->
 *             c.jdbcUrl = [...]
 *             c.username = [...]
 *             c.password = [...]
 *         }
 *     }
 * }
 * ```
 */
fun <T : Any> Modular.customize(
    bean: Bean<T>,
    block: BeanConsumerDefinition<T>.(T) -> Unit
) {
    bean.handler.onCustomize { instance ->
        val definition = BeanConsumerDefinition(
            container,
            instance
        )
        definition.block(instance)
    }
}