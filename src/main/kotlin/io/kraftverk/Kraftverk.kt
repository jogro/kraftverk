/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.createManagedModule
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Bootstrap
 */
class Kraftverk {
    companion object
}

/**
 * A factory function that creates a [Managed] instance of the specified [Module].
 * ```kotlin
 * val app: Managed<AppModule> = Kraftverk.manage { AppModule() }
 * ```
 * The function will by default perform the following actions:
 * 1) All [Value] bindings declared in the [Module] are eagerly looked up using the supplied [Environment].
 * Should any value be missing an exception is thrown.
 * 2) All [Bean] bindings are eagerly instantiated.
 *
 * Call the [Managed.destroy] method to destroy the [Managed] instance. Otherwise, this will be performed
 * by a shutdown hook.
 */
fun <M : Module> Kraftverk.Companion.manage(
    namespace: String = "",
    lazy: Boolean = false,
    environment: Environment = Environment.standard(),
    module: () -> M
): Managed<M> {
    contract {
        callsInPlace(module, InvocationKind.EXACTLY_ONCE)
    }
    return createManagedModule(namespace, lazy, environment, module).apply {
        Runtime.getRuntime().addShutdownHook(Thread {
            destroy()
        })
    }
}
