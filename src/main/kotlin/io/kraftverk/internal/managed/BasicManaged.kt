/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.managed

import io.kraftverk.internal.logging.createLogger
import io.kraftverk.internal.misc.Consumer
import io.kraftverk.internal.misc.applyAs
import io.kraftverk.internal.module.BasicModule

open class BasicManaged<M : BasicModule> internal constructor(
    module: M
) {
    internal val logger = createLogger { }

    @Volatile
    internal var state: State<M> = State.Defining(module)

    internal sealed class State<out M : BasicModule> {

        class Defining<M : BasicModule>(
            val module: M,
            var onStart: Consumer<M> = {}
        ) : State<M>()

        class Started<M : BasicModule>(
            val module: M
        ) : State<M>()

        object Destroying : State<Nothing>()

        object Destroyed : State<Nothing>()
    }

    internal val module: M
        get() {
            state.applyAs<State.Started<M>> {
                return module
            }
        }

    internal companion object
}
