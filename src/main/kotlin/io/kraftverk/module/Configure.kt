/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.handler
import io.kraftverk.declaration.BeanConfigurationDeclaration

/**
 * A helper method for configuring a bean after it has been declared, for example:
 * ```kotlin
 * class JdbcModule : Module() {
 *     [...]
 *     val dataSource by bean { HikariDataSource() }
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
fun <T : Any> BasicModule<*>.configure(
    bean: Bean<T>,
    block: BeanConfigurationDeclaration<T>.(T) -> Unit
) {
    bean.handler.configure { instance, lifecycle ->
        val definition = BeanConfigurationDeclaration(
            container,
            instance,
            lifecycle
        )
        definition.block(instance)
    }
}
