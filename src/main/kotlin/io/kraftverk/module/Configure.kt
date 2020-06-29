/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.CustomBean
import io.kraftverk.binding.handler
import io.kraftverk.declaration.ComponentConfigurationDeclaration

/**
 * A helper method for configuring a component after it has been declared, for example:
 * ```kotlin
 * class JdbcModule : Module() {
 *     [...]
 *     val dataSource by component { HikariDataSource() }
 *     init {
 *         configure(dataSource) { ds ->
 *             ds.jdbcUrl = [...]
 *             ds.username = [...]
 *             ds.password = [...]
 *         }
 *     }
 * }
 * ```
 */
fun <T : Any, S : Any> BasicModule<*>.configure(
    component: CustomBean<T, S>,
    block: ComponentConfigurationDeclaration<S>.(S) -> Unit
) {
    component.handler.configure { instance, lifecycle ->
        val callback = { s: S ->
            val definition = ComponentConfigurationDeclaration(
                container,
                s,
                lifecycle
            )
            definition.block(s)
        }
        component.handler.onConfigure(instance, callback)
    }
}

fun <T : Any> BasicModule<*>.configure(
    component: Bean<T>,
    block: ComponentConfigurationDeclaration<T>.(T) -> Unit
) {
    component.handler.configure { instance, lifecycle ->
        val callback = { s: T ->
            val definition = ComponentConfigurationDeclaration(
                container,
                s,
                lifecycle
            )
            definition.block(s)
        }
        component.handler.onConfigure(instance, callback)
    }
}
