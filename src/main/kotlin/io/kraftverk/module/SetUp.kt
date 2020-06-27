/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.CustomBean
import io.kraftverk.binding.handler
import io.kraftverk.declaration.ComponentSetupDeclaration

/**
 * A helper method for configuring a component after it has been declared, for example:
 * ```kotlin
 * class JdbcModule : Module() {
 *     [...]
 *     val dataSource by component { HikariDataSource() }
 *     init {
 *         setUp(dataSource) { ds ->
 *             ds.jdbcUrl = [...]
 *             ds.username = [...]
 *             ds.password = [...]
 *         }
 *     }
 * }
 * ```
 */
fun <T : Any, S : Any> BasicModule<*>.setUp(
    component: CustomBean<T, S>,
    block: ComponentSetupDeclaration<S>.(S) -> Unit
) {
    component.handler.setUp { instance, lifecycle ->
        val callback = { s: S ->
            val definition = ComponentSetupDeclaration(
                container,
                s,
                lifecycle
            )
            definition.block(s)
        }
        component.handler.onSetUp(instance, callback)
    }
}

fun <T : Any> BasicModule<*>.setUp(
    component: Bean<T>,
    block: ComponentSetupDeclaration<T>.(T) -> Unit
) {
    component.handler.setUp { instance, lifecycle ->
        val callback = { s: T ->
            val definition = ComponentSetupDeclaration(
                container,
                s,
                lifecycle
            )
            definition.block(s)
        }
        component.handler.onSetUp(instance, callback)
    }
}
