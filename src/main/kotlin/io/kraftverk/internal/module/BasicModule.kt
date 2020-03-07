package io.kraftverk.internal.module

import io.kraftverk.env.Environment
import io.kraftverk.internal.container.Container
import io.kraftverk.internal.container.start
import io.kraftverk.internal.container.stop

private val threadBoundContainer = ThreadBound<Container>()
private val threadBoundNamespace = ThreadBound<String>()

open class BasicModule {
    internal val container: Container = threadBoundContainer.get()
    internal val namespace: String = threadBoundNamespace.get()

    internal fun start() = container.start()
    internal fun stop() = container.stop()

    internal companion object
}

internal fun <M : BasicModule> createRootModule(
    lazy: Boolean = false,
    env: Environment,
    namespace: String,
    createModule: () -> M
): M {
    val container = Container(lazy, env)
    return threadBoundContainer.use(container) {
        threadBoundNamespace.use(namespace) {
            createModule()
        }
    }
}

internal fun <M : BasicModule> createSubModule(
    namespace: String,
    moduleFun: () -> M
): M = threadBoundNamespace.use(namespace) {
    moduleFun()
}

private class ThreadBound<T> {

    private val threadLocal = ThreadLocal<T>()

    fun get(): T = threadLocal.get() ?: throw IllegalStateException()

    fun <R> use(value: T, block: () -> R): R {
        val previous: T? = threadLocal.get()
        threadLocal.set(value)
        try {
            return block()
        } finally {
            if (previous == null) {
                threadLocal.remove()
            } else {
                threadLocal.set(previous)
            }
        }
    }
}
