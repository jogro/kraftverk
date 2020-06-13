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
fun <T : Any> AbstractModule.shape(
    bean: Bean<T>,
    block: BeanShapingDeclaration<T>.(T) -> Unit
) {
    bean.handler.onShape { instance, lifecycle ->
        val definition = BeanShapingDeclaration(
            container,
            instance,
            lifecycle
        )
        definition.block(instance)
    }
}
