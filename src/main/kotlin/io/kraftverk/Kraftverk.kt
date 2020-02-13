/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

/**
 * A factory function that starts a [Managed] instance of the specified implementation
 * [M] of [Module].
 * ```kotlin
 * val app: Managed<AppModule> = start { AppModule() }
 * ```
 * The function will by default perform the following actions:
 * 1) All [Value] bindings declared in the [Module] are eagerly looked up using the supplied [Environment].
 * Should any value be missing an exception is thrown.
 * 2) All [Bean] bindings are eagerly instantiated.
 *
 * Call the [Managed.destroy] method to destroy the [Managed] instance. Otherwise, shutdown will be performed
 * by a shutdown hook.
 */
fun <M : Module> start(
    namespace: String = "",
    lazy: Boolean = false,
    refreshable: Boolean = true,
    environment: Environment = Environment.standard(),
    module: () -> M
): Managed<M> {
    return manage(namespace, lazy, refreshable, environment, module).apply { start() }
}

fun <M : Module> manage(
    namespace: String = "",
    lazy: Boolean = false,
    refreshable: Boolean = true,
    environment: Environment = Environment.standard(),
    module: () -> M
): Managed<M> {
    val runtime = {
        val container = Container(lazy, refreshable, environment)
        ModuleRuntime(
            container = container,
            module = ModuleCreationContext.use(container, namespace) {
                module()
            }
        )
    }
    return Managed(runtime).apply {
        Runtime.getRuntime().addShutdownHook(Thread {
            destroy()
        })
    }
}

fun <M : Module> manageLazy(
    namespace: String = "",
    refreshable: Boolean = true,
    environment: Environment = Environment.standard(),
    module: () -> M
): Managed<M> {
    return manage(namespace, true, refreshable, environment, module)
}
