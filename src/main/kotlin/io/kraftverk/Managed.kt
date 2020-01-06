/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.Container
import io.kraftverk.internal.provider
import mu.KotlinLogging
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

private val logger = KotlinLogging.logger {}

/**
 * Provides access to and manages [Value] and [Bean] instances in a specified
 * implementation [M] of [Module].
 *
 * Given a module:
 * ```kotlin
 * class AppModule : Module() {
 *     val username by string()
 *     val someService by bean {
 *         SomeService(
 *             username()
 *         )
 *     }
 * }
 * ```
 * Call the [Kraftverk.Companion.manage] method to create a managed instance of the module.
 * ```kotlin
 * val app = Kraftverk.manage { AppModule() }
 * ```
 * To access the bean instance, call the [Managed.get]
 * method:
 * ```kotlin
 * val someService = app.get { someService }
 * ```
 */
class Managed<M : Module> internal constructor(
    internal val container: Container,
    internal val module: M
) {
    companion object
}

/**
 * Extracts instance [T] from the specified [binding].
 * ```kotlin
 * val someService = app.get { someService }
 * ```
 */
fun <M : Module, T : Any> Managed<M>.get(binding: M.() -> Binding<T>): T {
    contract {
        callsInPlace(binding, InvocationKind.EXACTLY_ONCE)
    }
    return module.binding().provider().instance()
}

/**
 * Refreshes this instance.
 */
fun <M : Module> Managed<M>.refresh() {
    container.refresh()
}

/**
 * Destroys this instance.
 */
fun <M : Module> Managed<M>.destroy() {
    container.destroy()
}
