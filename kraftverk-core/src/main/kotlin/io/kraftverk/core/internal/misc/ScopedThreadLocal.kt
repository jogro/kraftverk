package io.kraftverk.core.internal.misc

internal class ScopedThreadLocal<T> {

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
