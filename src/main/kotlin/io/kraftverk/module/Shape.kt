/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.CustomBean
import io.kraftverk.binding.handler
import io.kraftverk.declaration.ComponentShapingDeclaration
import io.kraftverk.internal.binding.onShape

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
fun <T : Any, S : Any> AbstractModule.shape(
    component: CustomBean<T, S>,
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

fun <T : Any> AbstractModule.shape(
    component: Bean<T>,
    block: ComponentShapingDeclaration<T>.(T) -> Unit
) {
    component.handler.onShape { instance, lifecycle ->
        val f = { s: T ->
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
