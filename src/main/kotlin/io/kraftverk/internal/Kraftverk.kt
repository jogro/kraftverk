package io.kraftverk.internal

import io.kraftverk.Environment
import io.kraftverk.Kraftverk
import io.kraftverk.Managed
import io.kraftverk.Module
import mu.KotlinLogging
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger {}

internal fun <M : Module> Kraftverk.Companion.createManagedModule(
    namespace: String = "",
    lazy: Boolean = false,
    env: Environment,
    module: () -> M
): Managed<M> {
    contract {
        callsInPlace(module, InvocationKind.EXACTLY_ONCE)
    }
    return measureTimedValue {
        logger.info("Creating managed module")
        val container = Container(lazy, env)
        val rootModule = ModuleCreationContext.use(container, namespace) { module() }
        container.start()
        Managed(container, rootModule)
    }.also {
        logger.info("Created managed module in ${it.duration}")
    }.value
}