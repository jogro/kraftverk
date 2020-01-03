/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.Module
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal class ModuleCreationContext {

    companion object {

        val container get() = threadBoundContainer.get()
        val namespace get() = threadBoundNamespace.get()

        private val threadBoundContainer = ThreadBound<Container>()
        private val threadBoundNamespace = ThreadBound<String>()

        internal fun <R> use(namespace: String, block: () -> R): R {
            return threadBoundNamespace.use(namespace, block)
        }

        internal fun <R> use(container: Container, block: () -> R): R {
            return threadBoundContainer.use(container, block)
        }

    }
}

internal fun <M : Module> ModuleCreationContext.Companion.use(
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