package io.kraftverk.managed.operations

import io.kraftverk.internal.managed.InternalManaged.State
import io.kraftverk.internal.misc.applyAs
import io.kraftverk.managed.Managed
import io.kraftverk.module.Module

/**
 * The [start] function will by default perform the following actions:
 * 1) All value bindings declared in the [Module] are eagerly looked up using the [Environment] that
 * was specified at the time the managed instance was created.
 * Should any value be missing an exception is thrown.
 * 2) All [Bean] bindings are eagerly instantiated.
 *
 * Call the [Managed.stop] method to destroy the [Managed] instance. Otherwise, shutdown will be performed
 * by a shutdown hook.
 */
fun <M : Module> Managed<M>.start(block: M.() -> Unit = {}): Managed<M> {
    logger.info { "Starting module" }
    val startMs = System.currentTimeMillis()
    customize(block)
    state.applyAs<State.Defining<M>> {
        val runtime = createRuntime()
        onStart(runtime.module)
        runtime.start()
        state = State.Started(runtime)
    }
    logger.info { "Started module in ${System.currentTimeMillis() - startMs}ms" }
    return this
}
