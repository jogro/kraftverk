/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import mu.KotlinLogging

/**
 * Bootstrap
 */
object Kraftverk {
    internal val logger = KotlinLogging.logger {}
}

/**
 * A factory function that starts a [Managed] instance of the specified implementation
 * [M] of [Module].
 * ```kotlin
 * val app: Managed<AppModule> = start { AppModule() }
 * ```
 * The function will by default perform the following actions:
 * 1) All [Value] bindings declared in the [Module] are eagerly looked up using the supplied [Environment].
 * Should any value be missing an exception is thrown.
 * 2) All [Bean] bindings are eagerly instantiated.
 *
 * Call the [Managed.destroy] method to destroy the [Managed] instance. Otherwise, shutdown will be performed
 * by a shutdown hook.
 */
fun <M : Module> Kraftverk.start(
    namespace: String = "",
    lazy: Boolean = false,
    refreshable: Boolean = true,
    env: Environment = environment(),
    module: () -> M
): Managed<M> {
    return manage(namespace, lazy, refreshable, env, module).apply { start() }
}

fun <M : Module> Kraftverk.manage(
    namespace: String = "",
    lazy: Boolean = false,
    refreshable: Boolean = true,
    env: Environment = environment(),
    module: () -> M
): Managed<M> {
    logger.info { "Creating managed module(lazy = $lazy, namespace = '$namespace')" }
    val runtime = {
        val container = Container(lazy, env)
        ModuleRuntime(
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
