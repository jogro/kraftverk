/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.managed

import io.kraftverk.common.ComponentProcessor
import io.kraftverk.common.ValueProcessor
import io.kraftverk.internal.container.Container
import io.kraftverk.internal.container.start
import io.kraftverk.internal.misc.mustBe
import io.kraftverk.module.Module
import io.kraftverk.module.createModule

/**
 * The [start] function will by default perform the following actions:
 * 1) All value bindings declared in the [Module] are eagerly looked up using the Environment that
 * was specified at the time the managed instance was created.
 * Should any value be missing an exception is thrown.
 * 2) All Component bindings are eagerly instantiated.
 *
 * Call the [Managed.stop] method to destroy the [Managed] instance.
 */
fun <M : Module> Managed<M>.start(block: M.() -> Unit = {}): Managed<M> {
    logger.info { "Starting managed module" }
    val startMs = System.currentTimeMillis()
    config(block)
    state.mustBe<Managed.State.Configurable<M>> {
        val module = createModule()
        onStart(module)
        module.container.start()
        state = Managed.State.Running(module)
    }
    Runtime.getRuntime().addShutdownHook(Thread {
        stop()
    })
    logger.info { "Started managed module in ${System.currentTimeMillis() - startMs}ms" }
    return this
}

fun <M : Module> Managed<M>.config(block: M.() -> Unit): Managed<M> {
    state.mustBe<Managed.State.Configurable<M>> {
        val previousOnStart = onStart
        onStart = { instance ->
            previousOnStart(instance)
            block(instance)
        }
    }
    return this
}

fun <M : Module> Managed<M>.registerProcessor(processor: ComponentProcessor): Managed<M> {
    state.mustBe<Managed.State.Configurable<M>> {
        componentProcessors += processor
    }
    return this
}

fun <M : Module> Managed<M>.registerProcessor(processor: ValueProcessor): Managed<M> {
    state.mustBe<Managed.State.Configurable<M>> {
        valueProcessors += processor
    }
    return this
}

private fun <M : Module> Managed.State.Configurable<M>.createModule(): M {
    val container = Container(lazy, env, componentProcessors, valueProcessors)
    return createModule(container, namespace, moduleFun)
}
