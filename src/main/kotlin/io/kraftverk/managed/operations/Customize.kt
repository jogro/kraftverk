package io.kraftverk.managed.operations

import io.kraftverk.internal.managed.InternalManaged.State
import io.kraftverk.internal.misc.applyAs
import io.kraftverk.managed.Managed
import io.kraftverk.module.Module

fun <M : Module> Managed<M>.customize(block: M.() -> Unit): Managed<M> {
    state.applyAs<State.Defining<M>> {
        val consumer = onStart
        onStart = { instance ->
            consumer(instance)
            block(instance)
        }
    }
    return this
}
