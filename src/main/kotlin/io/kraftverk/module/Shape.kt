/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.handler
import io.kraftverk.declaration.BeanShapingDeclaration
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
    bean: Bean<T, S>,
    block: BeanShapingDeclaration<S>.(S) -> Unit
) {
    bean.handler.onShape { instance, lifecycle ->
        val f = { s: S ->
            val definition = BeanShapingDeclaration(
                container,
                s,
                lifecycle
            )
            definition.block(s)
        }
        bean.handler.definition.onShape(instance, f)
    }
}
