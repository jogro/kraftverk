/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.env.Environment
import io.kraftverk.env.environment
import io.kraftverk.managed.Managed
import io.kraftverk.managed.ModuleFactory
import io.kraftverk.managed.start
import io.kraftverk.module.Module

object Kraftverk {

    /**
     * A shortcut factory function that creates and starts a [Managed] instance of the specified [Module].
     * in one shot.
     *
     * A common use case is to invoke this method directly from the main function.
     * ```kotlin
     * function main() {
     *     Kraftverk.start { AppModule() }
     * }
     * ```
     */
    fun <M : Module> start(
        lazy: Boolean = false,
        env: Environment = environment(),
        namespace: String = "",
        module: () -> M
    ): Managed<M> = manage(env, namespace, module).start(lazy)

    /**
     * A factory function that creates a [Managed] instance of the specified [Module].
     * ```kotlin
     * val app: Managed<AppModule> = Kraftverk.manage { AppModule() }
     * ```
     * The instance can be started by calling [Managed.start].
     */
    fun <M : Module> manage(
        env: Environment = environment(),
        namespace: String = "",
        module: () -> M
    ): Managed<M> {
        return Managed(ModuleFactory(env, namespace, module))
    }
}
