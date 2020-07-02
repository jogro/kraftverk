/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.delegate
import io.kraftverk.common.Pipe
import io.kraftverk.common.delegate
import io.kraftverk.declaration.BeanConfigurationDeclaration
import io.kraftverk.declaration.PipeDeclaration

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
    sink: Pipe<T>,
    block: PipeDeclaration<T>.(T) -> Unit
) {
    sink.delegate.configure { instance, lifecycle ->
        val definition = PipeDeclaration(
            container,
            instance,
            lifecycle
        )
        definition.block(instance)
    }
}
