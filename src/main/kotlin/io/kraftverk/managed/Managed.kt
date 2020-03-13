/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.managed

import io.kraftverk.internal.container.beanProviders
import io.kraftverk.internal.container.valueProviders
import io.kraftverk.internal.logging.createLogger
import io.kraftverk.internal.misc.BasicState
import io.kraftverk.internal.misc.Consumer
import io.kraftverk.module.Module
import io.kraftverk.provider.BeanProvider
import io.kraftverk.provider.ValueProvider

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
class Managed<M : Module> internal constructor(module: M) {

    /**
     * Retrieves all [BeanProvider]s.
     */
    val beanProviders: List<BeanProvider<*>> by lazy { module.container.beanProviders }

    /**
     * Retrieves all [ValueProvider]s.
     */
    val valueProviders: List<ValueProvider<*>> by lazy { module.container.valueProviders }

    internal val logger = createLogger { }

    @Volatile
    internal var state: State<M> = State.UnderConstruction(module)

    internal sealed class State<out M : Module> : BasicState {

        class UnderConstruction<M : Module>(
            val module: M,
            var onStart: Consumer<M> = {}
        ) : State<M>()

        class Running<M : Module>(
            val module: M
        ) : State<M>()

        object Destroying : State<Nothing>()

        object Destroyed : State<Nothing>()
    }

    internal companion object
}
