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
 * Provides access to and manages [Bean] and [Value] instances in a specified
 * implementation [M] of [Module].
 *
 * Given a module:
 * ```kotlin
 * class AppModule : Module() {
 *     val username by string()   //<-- Value binding
 *     val someService by bean {  //<-- Bean binding
 *         SomeService(
 *             username()
 *         )
 *     }
 * }
 * ```
 * Call the [Kraftverk.Companion.manage] method to create a [Managed] instance of the module.
 * ```kotlin
 * val app: Managed<AppModule> = Kraftverk.manage { AppModule() }
 * ```
 */
class Managed<M : Module> internal constructor(
    internal val container: Container,
    internal val module: M
) {
    companion object
}

/**
 * Extracts instance [T] from the specified [Binding].
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
 * Destroys this instance.
 */
fun <M : Module> Managed<M>.destroy() {
    container.destroy()
}
