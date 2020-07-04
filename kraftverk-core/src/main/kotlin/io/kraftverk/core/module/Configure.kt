/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.core.module

import io.kraftverk.core.binding.Bean
import io.kraftverk.core.binding.delegate
import io.kraftverk.core.common.Pipe
import io.kraftverk.core.common.delegate
import io.kraftverk.core.declaration.BeanConfigurationDeclaration
import io.kraftverk.core.declaration.PipeDeclaration

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
    bean.delegate.configure { instance, lifecycle ->
        val definition = BeanConfigurationDeclaration(
            container,
            instance,
            lifecycle
        )
        definition.block(instance)
    }
}

fun <T : Any> BasicModule<*>.configure(
    pipe: Pipe<T>,
    block: PipeDeclaration<T>.(T) -> Unit
) {
    pipe.delegate.configure { instance, lifecycle ->
        val definition = PipeDeclaration(
            container,
            instance,
            lifecycle
        )
        definition.block(instance)
    }
}
