package io.kraftverk

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

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
 * Call the [start] method to create a [Managed] instance of the module.
 * ```kotlin
 * val app: Managed<AppModule> = start { AppModule() }
 * ```
 */
class Managed<M : Module> internal constructor(
    runtime: () -> ModuleRuntime<M>
) {
    @Volatile
    internal var state: State<M> = State.Defining(runtime)

    internal sealed class State<out M : Module> {

        class Defining<M : Module>(
            val runtime: () -> ModuleRuntime<M>,
            var onStart: Consumer<M> = {}
        ) : State<M>()

        class Running<M : Module>(
            val runtime: ModuleRuntime<M>
        ) : State<M>()

        object Destroyed : State<Nothing>()

    }

    companion object
}

fun <M : Module> Managed<M>.start(block: M.() -> Unit = {}): Managed<M> {
    configure(block)
    state.applyAs<Managed.State.Defining<M>> {
        val runtime = runtime()
        onStart(runtime.module)
        state = Managed.State.Running(runtime)
        runtime.start()
    }
    return this
}

/**
 * Extracts instance [T] from the specified [Binding].
 * ```kotlin
 * val someService = app { someService }
 * ```
 */
operator fun <M : Module, T : Any> Managed<M>.invoke(binding: M.() -> Binding<T>): T {
    return module.binding().provider.get()
}

/**
 * Lazy extraction of instance [T] from the specified [Binding].
 * ```kotlin
 * val someService by app.get { someService }
 * ```
 */
fun <M : Module, T : Any> Managed<M>.get(binding: M.() -> Binding<T>) =
    object : ReadOnlyProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return module.binding().provider.get()
        }
    }

/**
 * Refreshes this managed [Module]. All [Value]s and [Bean]s will be destroyed and reinitialized.
 *
 * It is possible to specify whether a certain bean is refreshable, see the [bean] declaration method.
 */
fun <M : Module> Managed<M>.refresh(): Managed<M> {
    state.applyWhen<Managed.State.Running<*>> {
        runtime.refresh()
    }
    return this
}

/**
 * Destroys this instance meaning that all [Bean]s will be destroyed.
 */
fun <M : Module> Managed<M>.destroy() {
    state.applyWhen<Managed.State.Running<*>> {
        runtime.destroy()
        state = Managed.State.Destroyed
    }
}

fun <M : Module> Managed<M>.configure(block: M.() -> Unit): Managed<M> {
    state.applyAs<Managed.State.Defining<M>> {
        val consumer = onStart
        onStart = { instance ->
            consumer(instance)
            block(instance)
        }
    }
    return this
}

internal val <M : Module> Managed<M>.module: M
    get() {
        state.applyAs<Managed.State.Running<M>> {
            return runtime.module
        }
    }

internal class ModuleRuntime<out M : Module>(val container: Container, val module: M)

private fun ModuleRuntime<*>.start() = container.start()
private fun ModuleRuntime<*>.destroy() = container.destroy()
private fun ModuleRuntime<*>.refresh() = container.refresh()
