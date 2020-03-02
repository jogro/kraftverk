package io.kraftverk.internal.module

import io.kraftverk.internal.container.Container
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal class ModuleCreationContext {

    companion object {

        val container get() = threadBoundContainer.get()
        val namespace get() = threadBoundNamespace.get()

        private val threadBoundContainer =
            ThreadBound<Container>()
        private val threadBoundNamespace = ThreadBound<String>()

        internal fun <R> use(namespace: String, block: () -> R): R {
            return threadBoundNamespace.use(namespace, block)
        }

        internal fun <R> use(container: Container, block: () -> R): R {
            return threadBoundContainer.use(container, block)
        }
    }
}

internal fun <M : InternalModule> ModuleCreationContext.Companion.use(
    container: Container,
    namespace: String,
    moduleFun: () -> M
): M {
    contract {
        callsInPlace(moduleFun, InvocationKind.EXACTLY_ONCE)
    }
    return use(container) {
        use(namespace) {
            moduleFun()
        }
    }
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
