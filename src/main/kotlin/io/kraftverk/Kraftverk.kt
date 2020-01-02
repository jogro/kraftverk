package io.kraftverk

import io.kraftverk.internal.createManagedModule
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class Kraftverk {
    companion object
}

/**
 * Creates a managed instance [M] of [Module].
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
 * val app: Managed<AppModule> = Kraftverk.manage { AppModule() }
 * ```
 * To access the bean instance, call the [Managed.get]
 * method:
 * ```kotlin
 * val someService = app.get { someService }
 * ```
 */
fun <M : Module> Kraftverk.Companion.manage(
    namespace: String = "",
    lazy: Boolean = false,
    env: Environment = Environment.standard(),
    module: () -> M
): Managed<M> {
    contract {
        callsInPlace(module, InvocationKind.EXACTLY_ONCE)
    }
    return createManagedModule(namespace, lazy, env, module).apply {
        Runtime.getRuntime().addShutdownHook(Thread {
            destroy()
        })
    }
}
