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
 * Provides access to and manages [Property] and [Bean] instances in a specified
 * implementation [M] of [Module].
 *
 * Given a module:
 * ```kotlin
 * class AppModule : Module() {
 *     val username by stringProperty()
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
 * Contains the active profiles set at startup.
 *
 * To set the active profiles, you can use
 * - an environment variable named 'KRAFTVERK_ACTIVE_PROFILES'
 * - a system property named 'kraftverk.active.profiles'
 *
 * The value provided should be a comma delimited string
 * containing the names of the profiles.
 *
 */
val Managed<*>.profiles: List<String> get() = container.profiles

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
