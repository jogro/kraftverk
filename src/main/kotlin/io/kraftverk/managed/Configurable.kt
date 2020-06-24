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
import io.kraftverk.managed.Managed.State.Configurable
import io.kraftverk.managed.Managed.State.Running
import io.kraftverk.module.Module
import io.kraftverk.module.createModule

/**
 * The [start] function is by default non-lazy, meaning that:
 * 1) All value bindings declared in the [Module] are eagerly looked up using the Environment that
 * was specified at the time the managed instance was created. Should any value be missing an exception is thrown.
 * 2) All Component bindings are eagerly instantiated.
 *
 * Call the [Managed.stop] method to destroy the [Managed] instance.
 */
fun <M : Module> Managed<M>.start(lazy: Boolean = false, block: M.() -> Unit = {}): Managed<M> {
    logger.info { "Starting managed module" }
    val startMs = System.currentTimeMillis()
    shape(block)
    state.mustBe<Configurable<M>> {
        val module = createModule()
        onShape(module)
        module.container.start(lazy)
        state = Running(module)
    }
    Runtime.getRuntime().addShutdownHook(Thread { stop() })
    logger.info { "Started managed module in ${System.currentTimeMillis() - startMs}ms" }
    return this
}

fun <M : Module> Managed<M>.shape(block: M.() -> Unit): Managed<M> {
    state.mustBe<Configurable<M>> {
        val previousOnShape = onShape
        onShape = { module ->
            previousOnShape(module)
            block(module)
        }
    }
    return this
}

fun <M : Module> Managed<M>.addProcessor(processor: ComponentProcessor): Managed<M> {
    state.mustBe<Configurable<M>> {
        componentProcessors += processor
    }
    return this
}

fun <M : Module> Managed<M>.addProcessor(processor: ValueProcessor): Managed<M> {
    state.mustBe<Configurable<M>> {
        valueProcessors += processor
    }
    return this
}

private fun <M : Module> Configurable<M>.createModule(): M {
    val container = Container(env, componentProcessors, valueProcessors)
    return createModule(container, namespace, moduleFun)
}
