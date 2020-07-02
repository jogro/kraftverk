/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.core.managed

import io.kraftverk.core.internal.logging.createLogger
import io.kraftverk.core.internal.misc.BasicState
import io.kraftverk.core.module.Module

/**
 * Provides access to and manages bean and value instances in a specified
 * implementation [M] of [Module].
 *
 * Given a module:
 * ```kotlin
 * class AppModule : Module() {
 *     val username by string()   //<-- Value binding
 *     val someService by bean {  //<-- Bean binding
 *         SomeService(
 *             username()
 *         )
 *     }
 * }
 * ```
 * Call the manage factory function to create a [Managed] instance of the module.
 * ```kotlin
 * val app by Kraftverk.manage { AppModule() }
 * ```
 */
class Managed<M : Module> internal constructor(moduleFactory: ModuleFactory<M>) {

    internal val logger = createLogger { }

    @Volatile
    internal var state: State<M> =
        State.Configurable(moduleFactory)

    internal sealed class State<out M : Module> :
        BasicState {

        class Configurable<M : Module>(
            val moduleFactory: ModuleFactory<M>
        ) : State<M>()

        class Running<M : Module>(
            val module: M
        ) : State<M>()

        object Destroying : State<Nothing>()

        object Destroyed : State<Nothing>()
    }

    internal companion object
}
