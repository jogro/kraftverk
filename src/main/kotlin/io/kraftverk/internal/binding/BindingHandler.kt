package io.kraftverk.internal.binding

import io.kraftverk.internal.misc.Consumer
import io.kraftverk.internal.misc.InstanceFactory
import io.kraftverk.internal.misc.ProviderFactory
import io.kraftverk.internal.misc.applyAs
import io.kraftverk.internal.misc.applyWhen

internal class BindingHandler<T : Any>(
    createInstance: InstanceFactory<T>,
    createProvider: ProviderFactory<T>
) {

    @Volatile
    internal var state: State<T> =
        State.Defining(createInstance, createProvider)

    internal sealed class State<out T : Any> {

        class Defining<T : Any>(
            createInstance: InstanceFactory<T>,
            val createProvider: ProviderFactory<T>
        ) : State<T>() {
            var create: InstanceFactory<T> = createInstance
            var onCreate: Consumer<T> = {}
            var onDestroy: Consumer<T> = {}
        }

        class Running<T : Any>(
            val provider: Provider<T>
        ) : State<T>()

        object Destroyed : State<Nothing>()
    }
}

internal fun <T : Any> BindingHandler<T>.onBind(
    block: (InstanceFactory<T>) -> T
) {
    state.applyAs<BindingHandler.State.Defining<T>> {
        val supplier = create
        create = {
            block(supplier)
        }
    }
}

internal fun <T : Any> BindingHandler<T>.onCreate(
    block: (T, Consumer<T>) -> Unit
) {
    state.applyAs<BindingHandler.State.Defining<T>> {
        val consumer = onCreate
        onCreate = { instance ->
            block(instance, consumer)
        }
    }
}

internal fun <T : Any> BindingHandler<T>.onDestroy(
    block: (T, Consumer<T>) -> Unit
) {
    state.applyAs<BindingHandler.State.Defining<T>> {
        val consumer = onDestroy
        onDestroy = { instance ->
            block(instance, consumer)
        }
    }
}

internal fun <T : Any> BindingHandler<T>.start() {
    state.applyAs<BindingHandler.State.Defining<T>> {
        val provider = createProvider(
            create,
            onCreate,
            onDestroy
        )
        state = BindingHandler.State.Running(provider)
    }
}

internal fun BindingHandler<*>.initialize() =
    state.applyAs<BindingHandler.State.Running<*>> {
        provider.initialize()
    }

internal val <T : Any> BindingHandler<T>.provider: Provider<T>
    get() {
        state.applyAs<BindingHandler.State.Running<T>> {
            return provider
        }
    }

internal fun BindingHandler<*>.stop() =
    state.applyWhen<BindingHandler.State.Running<*>> {
        provider.destroy()
        state = BindingHandler.State.Destroyed
    }
