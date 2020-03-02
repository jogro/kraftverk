/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.env.Environment
import io.kraftverk.env.environment
import io.kraftverk.internal.container.Container
import io.kraftverk.internal.module.ModuleCreationContext
import io.kraftverk.internal.module.use
import io.kraftverk.managed.Managed
import io.kraftverk.managed.ManagedRuntime
import io.kraftverk.managed.destroy
import io.kraftverk.module.Module
import mu.KotlinLogging

/**
 * Bootstrap
 */
object Kraftverk {
    internal val logger = KotlinLogging.logger {}
}

/**
 * A factory function that creates a [Managed] instance of the specified [Module].
  * ```kotlin
 * val app: Managed<AppModule> = Kraftverk.manage { AppModule() }
 * ```
 */
fun <M : Module> Kraftverk.manage(
    namespace: String = "",
    lazy: Boolean = false,
    env: Environment = environment(),
    module: () -> M
): Managed<M> {
    logger.info { "Creating managed module(lazy = $lazy, namespace = '$namespace')" }
    val runtime = {
        val container = Container(lazy, env)
        ManagedRuntime(
            container = container,
            module = ModuleCreationContext.use(container, namespace) {
                module()
            }
        )
    }
    return Managed(runtime).apply {
        Runtime.getRuntime().addShutdownHook(Thread {
            destroy()
        })
    }
}
