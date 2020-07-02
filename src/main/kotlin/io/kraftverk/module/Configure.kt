/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.delegate
import io.kraftverk.common.Sink
import io.kraftverk.common.delegate
import io.kraftverk.declaration.SinkDeclaration

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
    block: SinkDeclaration<T>.(T) -> Unit
) {
    bean.delegate.configure { instance, lifecycle ->
        val definition = SinkDeclaration(
            container,
            instance,
            lifecycle
        )
        definition.block(instance)
    }
}

fun <T : Any> BasicModule<*>.configure(
    sink: Sink<T>,
    block: SinkDeclaration<T>.(T) -> Unit
) {
    sink.delegate.configure { instance, lifecycle ->
        val definition = SinkDeclaration(
            container,
            instance,
            lifecycle
        )
        definition.block(instance)
    }
}
