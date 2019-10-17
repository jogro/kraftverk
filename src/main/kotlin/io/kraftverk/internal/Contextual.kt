/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

/**
 *  Inspired by https://github.com/ceylon/ceylon.language/blob/master/src/ceylon/language/Contextual.ceylon
 */
class Contextual<T> {

    private val threadLocal = ThreadLocal<T>()

    fun get(): T = threadLocal.get() ?: throw AssertionError()

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