package io.kraftverk.internal.managed

import io.kraftverk.internal.container.Container
import io.kraftverk.internal.container.start
import io.kraftverk.internal.container.stop
import io.kraftverk.internal.misc.Consumer
import io.kraftverk.internal.misc.applyAs
import io.kraftverk.internal.module.InternalModule
import mu.KotlinLogging

open class InternalManaged<M : InternalModule> internal constructor(
    createRuntime: () -> Runtime<M>
) {
    internal val logger = KotlinLogging.logger {}

    @Volatile
    internal var state: State<M> = State.Defining(createRuntime)

    internal sealed class State<out M : InternalModule> {

        class Defining<M : InternalModule>(
            val createRuntime: () -> Runtime<M>,
            var onStart: Consumer<M> = {}
        ) : State<M>()

        class Started<M : InternalModule>(
            val runtime: Runtime<M>
        ) : State<M>()

        object Destroying : State<Nothing>()

        object Destroyed : State<Nothing>()
    }

    internal class Runtime<out M : InternalModule>(val container: Container, val module: M)

    internal fun Runtime<*>.start() = container.start()
    internal fun Runtime<*>.stop() = container.stop()

    internal val module: M
        get() {
            state.applyAs<State.Started<M>> {
                return runtime.module
            }
        }

    internal companion object
}
