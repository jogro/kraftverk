/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.core.managed

import io.kraftverk.core.common.BeanProcessor
import io.kraftverk.core.common.ValueProcessor
import io.kraftverk.core.internal.container.start
import io.kraftverk.core.internal.misc.mustBe
import io.kraftverk.core.managed.Managed.State.Configurable
import io.kraftverk.core.managed.Managed.State.Running
import io.kraftverk.core.module.Module

/**
 * The [start] function is by default non-lazy, meaning that:
 * 1) All value bindings declared in the [Module] are eagerly looked up using the Environment that
 * was specified at the time the managed instance was created. Should any value be missing an exception is thrown.
 * 2) All Bean bindings are eagerly instantiated.
 *
 * Call the [Managed.stop] method to destroy the [Managed] instance.
 */
fun <M : Module> Managed<M>.start(lazy: Boolean = false, block: M.() -> Unit = {}): Managed<M> {
    logger.info { "Starting managed module" }
    val startMs = System.currentTimeMillis()
    state.mustBe<Configurable<M>> {
        moduleFactory.configure(block)
        val module = moduleFactory.createModule(lazy)
        state = Running(module)
    }
    Runtime.getRuntime().addShutdownHook(Thread { stop() })
    logger.info { "Started managed module in ${System.currentTimeMillis() - startMs}ms" }
    return this
}

fun <M : Module> Managed<M>.configure(block: M.() -> Unit): Managed<M> {
    state.mustBe<Configurable<M>> {
        moduleFactory.configure(block)
    }
    return this
}

fun <M : Module> Managed<M>.addProcessor(processor: BeanProcessor): Managed<M> {
    state.mustBe<Configurable<M>> {
        moduleFactory.addProcessor(processor)
    }
    return this
}

fun <M : Module> Managed<M>.addProcessor(processor: ValueProcessor): Managed<M> {
    state.mustBe<Configurable<M>> {
        moduleFactory.addProcessor(processor)
    }
    return this
}
