/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.XBean
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
    bean: XBean<T, S>,
    block: ComponentShapingDeclaration<S>.(S) -> Unit
) {
    bean.handler.onShape { instance, lifecycle ->
        val f = { s: S ->
            val definition = ComponentShapingDeclaration(
                container,
                s,
                lifecycle
            )
            definition.block(s)
        }
        bean.handler.definition.onShape(instance, f)
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
