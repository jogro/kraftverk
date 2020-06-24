/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.managed

import io.kraftverk.common.ComponentProcessor
import io.kraftverk.common.ValueProcessor
import io.kraftverk.env.Environment
import io.kraftverk.internal.logging.createLogger
import io.kraftverk.internal.misc.BasicState
import io.kraftverk.internal.misc.Consumer
import io.kraftverk.module.Module

/**
 * Provides access to and manages component and value instances in a specified
 * implementation [M] of [Module].
 *
 * Given a module:
 * ```kotlin
 * class AppModule : Module() {
 *     val username by string()   //<-- Value binding
 *     val someService by component {  //<-- Component binding
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
class Managed<M : Module> internal constructor(
    env: Environment,
    namespace: String,
    moduleFun: () -> M
) {

    internal val logger = createLogger { }

    @Volatile
    internal var state: State<M> = State.Configurable(
        moduleFun,
        env,
        namespace
    )

    internal sealed class State<out M : Module> : BasicState {

        class Configurable<M : Module>(
            val moduleFun: () -> M,
            val env: Environment,
            val namespace: String
        ) : State<M>() {
            val componentProcessors = mutableListOf<ComponentProcessor>()
            val valueProcessors = mutableListOf<ValueProcessor>()
            var onShape: Consumer<M> = {}
        }

        class Running<M : Module>(
            val module: M
        ) : State<M>()

        object Destroying : State<Nothing>()

        object Destroyed : State<Nothing>()
    }

    internal companion object
}
