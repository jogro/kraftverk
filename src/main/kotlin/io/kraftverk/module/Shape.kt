/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.CustomBean
import io.kraftverk.binding.handler
import io.kraftverk.declaration.ComponentShapeDeclaration

/**
 * A helper method for configuring a component after it has been declared, for example:
 * ```kotlin
 * class JdbcModule : Module() {
 *     [...]
 *     val dataSource by component { HikariDataSource() }
 *     init {
 *         shape(dataSource) { ds ->
 *             ds.jdbcUrl = [...]
 *             ds.username = [...]
 *             ds.password = [...]
 *         }
 *     }
 * }
 * ```
 */
fun <T : Any, S : Any> BasicModule<*>.shape(
    component: CustomBean<T, S>,
    block: ComponentShapeDeclaration<S>.(S) -> Unit
) {
    component.handler.shape { instance, lifecycle ->
        val callback = { s: S ->
            val definition = ComponentShapeDeclaration(
                container,
                s,
                lifecycle
            )
            definition.block(s)
        }
        component.handler.onShape(instance, callback)
    }
}

fun <T : Any> BasicModule<*>.shape(
    component: Bean<T>,
    block: ComponentShapeDeclaration<T>.(T) -> Unit
) {
    component.handler.shape { instance, lifecycle ->
        val callback = { s: T ->
            val definition = ComponentShapeDeclaration(
                container,
                s,
                lifecycle
            )
            definition.block(s)
        }
        component.handler.onShape(instance, callback)
    }
}
