package io.kraftverk.managed.operations

import io.kraftverk.internal.managed.InternalManaged.State
import io.kraftverk.internal.misc.applyWhen
import io.kraftverk.managed.Managed
import io.kraftverk.module.Module

/**
 * Stops this instance meaning that all beans will be destroyed.
 */
fun <M : Module> Managed<M>.stop() {
    state.applyWhen<State.Started<*>> {
        state = State.Destroying
        runtime.stop()
        state = State.Destroyed
    }
}
