package io.kraftverk.managed

import io.kraftverk.binding.Binding
import io.kraftverk.binding.provider
import io.kraftverk.internal.container.Container
import io.kraftverk.internal.container.destroy
import io.kraftverk.internal.container.start
import io.kraftverk.internal.misc.Consumer
import io.kraftverk.internal.misc.applyAs
import io.kraftverk.internal.misc.applyWhen
import io.kraftverk.manage
import io.kraftverk.module.Module
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import mu.KotlinLogging

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
 * Call the [manage] factory function to create a [Managed] instance of the module.
 * ```kotlin
 * val app by Kraftverk.manage { AppModule() }
 * ```
 */
class Managed<M : Module> internal constructor(
    runtime: () -> ManagedRuntime<M>
) {
    internal val logger = KotlinLogging.logger {}

    @Volatile
    internal var state: State<M> = State.Defining(runtime)

    internal sealed class State<out M : Module> {

        class Defining<M : Module>(
            val runtime: () -> ManagedRuntime<M>,
            var onStart: Consumer<M> = {}
        ) : State<M>()

        class Running<M : Module>(
            val runtime: ManagedRuntime<M>
        ) : State<M>()

        object Destroyed : State<Nothing>()
    }

    companion object
}

/**
 * The [start] function will by default perform the following actions:
 * 1) All [Value] binding declared in the [Module] are eagerly looked up using the [Environment] that
 * was specified at the time the managed instance was created.
 * Should any value be missing an exception is thrown.
 * 2) All [Bean] bindings are eagerly instantiated.
 *
 * Call the [Managed.destroy] method to destroy the [Managed] instance. Otherwise, shutdown will be performed
 * by a shutdown hook.
 */
fun <M : Module> Managed<M>.start(block: M.() -> Unit = {}): Managed<M> {
    logger.info { "Starting module" }
    val startMs = System.currentTimeMillis()
    customize(block)
    state.applyAs<Managed.State.Defining<M>> {
        val runtime = runtime()
        onStart(runtime.module)
        state = Managed.State.Running(runtime)
        runtime.start()
    }
    logger.info { "Started module in ${System.currentTimeMillis() - startMs}ms" }
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
 * Destroys this instance meaning that all [Bean]s will be destroyed.
 */
fun <M : Module> Managed<M>.destroy() {
    state.applyWhen<Managed.State.Running<*>> {
        runtime.destroy()
        state = Managed.State.Destroyed
    }
}

fun <M : Module> Managed<M>.customize(block: M.() -> Unit): Managed<M> {
    state.applyAs<Managed.State.Defining<M>> {
        val consumer = onStart
        onStart = { instance ->
            consumer(instance)
            block(instance)
        }
    }
    return this
}

internal class ManagedRuntime<out M : Module>(val container: Container, val module: M)

internal fun ManagedRuntime<*>.start() = container.start()
internal fun ManagedRuntime<*>.destroy() = container.destroy()

internal val <M : Module> Managed<M>.module: M
    get() {
        state.applyAs<Managed.State.Running<M>> {
            return runtime.module
        }
    }
