package io.kraftverk

import io.kraftverk.internal.createManagedModule
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class Kraftverk {
    companion object
}

/**
 * Creates a managed instance [M] of [Module].
 * ```kotlin
 * val app: Managed<AppModule> = Kraftverk.manage { AppModule() }
 * ```
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
