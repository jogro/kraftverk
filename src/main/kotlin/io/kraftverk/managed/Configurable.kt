/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.managed

import io.kraftverk.common.BeanProcessor
import io.kraftverk.common.ValueProcessor
import io.kraftverk.internal.container.start
import io.kraftverk.internal.misc.mustBe
import io.kraftverk.module.Module
import io.kraftverk.module.createRootModule

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
    state.mustBe<Managed.State.Configurable<M>> {
        val module = createRootModule()
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
    state.mustBe<Managed.State.Configurable<M>> {
        val previousOnStart = onStart
        onStart = { instance ->
            previousOnStart(instance)
            block(instance)
        }
    }
    return this
}

fun <M : Module> Managed<M>.registerProcessor(processor: BeanProcessor): Managed<M> {
    state.mustBe<Managed.State.Configurable<M>> {
        beanProcessors += processor
    }
    return this
}

fun <M : Module> Managed<M>.registerProcessor(processor: ValueProcessor): Managed<M> {
    state.mustBe<Managed.State.Configurable<M>> {
        valueProcessors += processor
    }
    return this
}

private fun <M : Module> Managed.State.Configurable<M>.createRootModule(): M =
    createRootModule(
        lazy, env, namespace, beanProcessors, valueProcessors, moduleFun
    )
