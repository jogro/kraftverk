package io.kraftverk.managed

import io.kraftverk.internal.container.start
import io.kraftverk.internal.misc.mustBe
import io.kraftverk.module.Module

/**
 * The [start] function will by default perform the following actions:
 * 1) All value bindings declared in the [Module] are eagerly looked up using the Environment that
 * was specified at the time the managed instance was created.
 * Should any value be missing an exception is thrown.
 * 2) All Bean bindings are eagerly instantiated.
 *
 * Call the [Managed.stop] method to destroy the [Managed] instance.
 */
fun <M : Module> Managed<M>.start(block: M.() -> Unit = {}): Managed<M> {
    logger.info { "Starting root module" }
    val startMs = System.currentTimeMillis()
    customize(block)
    state.mustBe<Managed.State.UnderConstruction<M>> {
        onStart(module)
        module.container.start()
        state = Managed.State.Running(module)
    }
    Runtime.getRuntime().addShutdownHook(Thread {
        stop()
    })
    logger.info { "Started root module in ${System.currentTimeMillis() - startMs}ms" }
    return this
}

fun <M : Module> Managed<M>.customize(block: M.() -> Unit): Managed<M> {
    state.mustBe<Managed.State.UnderConstruction<M>> {
        val previousOnStart = onStart
        onStart = { instance ->
            previousOnStart(instance)
            block(instance)
        }
    }
    return this
}
