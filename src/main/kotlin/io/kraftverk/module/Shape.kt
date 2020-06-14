/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Component
import io.kraftverk.binding.handler
import io.kraftverk.declaration.ComponentShapingDeclaration
import io.kraftverk.internal.binding.onShape

/**
 * A helper method for configuring a bean after it has been declared, for example:
 * ```kotlin
 * class JdbcModule : Module() {
 *     [...]
 *     val dataSource by bean { HikariDataSource() }
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
fun <T : Any, S : Any> AbstractModule.shape(
    component: Component<T, S>,
    block: ComponentShapingDeclaration<S>.(S) -> Unit
) {
    component.handler.onShape { instance, lifecycle ->
        val f = { s: S ->
            val definition = ComponentShapingDeclaration(
                container,
                s,
                lifecycle
            )
            definition.block(s)
        }
        component.handler.definition.onShape(instance, f)
    }
}
