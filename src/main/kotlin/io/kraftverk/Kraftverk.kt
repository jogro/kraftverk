/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import mu.KotlinLogging
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.measureTimedValue

/**
 * Bootstrap
 */
class Kraftverk {
    companion object
}

/**
 * A factory function that creates a [Managed] instance of the specified implementation
 * [M] of [Module].
 * ```kotlin
 * val app: Managed<AppModule> = Kraftverk.manage { AppModule() }
 * ```
 * The function will by default perform the following actions:
 * 1) All [Value] bindings declared in the [Module] are eagerly looked up using the supplied [Environment].
 * Should any value be missing an exception is thrown.
 * 2) All [Bean] bindings are eagerly instantiated.
 *
 * Call the [Managed.destroy] method to destroy the [Managed] instance. Otherwise, shutdown will be performed
 * by a shutdown hook.
 */
fun <M : Module> Kraftverk.Companion.manage(
    namespace: String = "",
    lazy: Boolean = false,
    refreshable: Boolean = true,
    environment: Environment = Environment.standard(),
    module: () -> M
): Managed<M> {
    contract {
        callsInPlace(module, InvocationKind.EXACTLY_ONCE)
    }
    return createManagedModule(namespace, lazy, refreshable, environment, module).apply {
        Runtime.getRuntime().addShutdownHook(Thread {
            destroy()
        })
    }
}

private val logger = KotlinLogging.logger {}

internal fun <M : Module> Kraftverk.Companion.createManagedModule(
    namespace: String,
    lazy: Boolean,
    refreshable: Boolean,
    environment: Environment,
    module: () -> M
): Managed<M> {
    contract {
        callsInPlace(module, InvocationKind.EXACTLY_ONCE)
    }
    return measureTimedValue {
        logger.info("Creating managed module")
        val container = Container(lazy, refreshable, environment)
        val rootModule = ModuleCreationContext.use(container, namespace) { module() }
        container.start()
        Managed(container, rootModule)
    }.also {
        logger.info("Created managed module in ${it.duration}")
    }.value
}
