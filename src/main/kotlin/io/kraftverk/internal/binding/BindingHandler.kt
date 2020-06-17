/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.binding

import io.kraftverk.common.BindingDefinition
import io.kraftverk.common.ComponentDefinition
import io.kraftverk.common.ValueDefinition
import io.kraftverk.declaration.LifecycleActions
import io.kraftverk.internal.logging.createLogger
import io.kraftverk.internal.misc.BasicState
import io.kraftverk.internal.misc.Supplier
import io.kraftverk.internal.provider.Singleton
import io.kraftverk.provider.ComponentProvider
import io.kraftverk.provider.ComponentProviderImpl
import io.kraftverk.provider.Provider
import io.kraftverk.provider.ValueProvider
import io.kraftverk.provider.ValueProviderImpl

internal sealed class BindingHandler<T : Any, S : Any, out P : Provider<T>>(
    instance: Supplier<T>
) {

    @Volatile
    internal var state: State<T> = State.Configurable<T, S>(instance)

    abstract fun createProvider(state: State.Configurable<T, S>): P

    internal sealed class State<out T : Any> : BasicState {

        data class Configurable<T : Any, S : Any>(
            var instance: Supplier<T>,
            var onShape: (T, LifecycleActions) -> Unit = { _, _ -> }
        ) : State<T>()

        data class Running<T : Any, P : Provider<T>>(
            val provider: P
        ) : State<T>()

        object Destroyed : State<Nothing>()
    }
}

internal class ComponentHandler<T : Any, S : Any>(
    val definition: ComponentDefinition<T, S>
) : BindingHandler<T, S, ComponentProvider<T, S>>(definition.instance) {

    private val logger = createLogger { }

    override fun createProvider(state: State.Configurable<T, S>) = ComponentProviderImpl(
        definition,
        createSingleton(
            definition,
            state.copy(
                instance = loggingInterceptor(state.instance)
            )
        )
    )

    private fun loggingInterceptor(block: () -> T): () -> T = {
        val startMs = System.currentTimeMillis()
        val t = block()
        val elapsed = System.currentTimeMillis() - startMs
        logger.info {
            "Component '${definition.name}' is bound to ${definition.type} (${elapsed}ms)"
        }
        t
    }
}

internal class ValueHandler<T : Any, S : Any>(
    val definition: ValueDefinition<T>
) : BindingHandler<T, S, ValueProvider<T>>(definition.instance) {

    private val logger = createLogger { }

    override fun createProvider(state: State.Configurable<T, S>) = ValueProviderImpl(
        definition,
        createSingleton(
            definition,
            state.copy(
                instance = loggingInterceptor(state.instance)
            )
        )
    )

    private fun loggingInterceptor(block: () -> T): () -> T = {
        val t = block()
        if (definition.secret) {
            logger.info { "Value '${definition.name}' is bound to '********'" }
        } else {
            logger.info { "Value '${definition.name}' is bound to '$t'" }
        }
        t
    }
}

private fun <T : Any, S : Any> createSingleton(
    definition: BindingDefinition<T>,
    state: BindingHandler.State.Configurable<T, S>
): Singleton<T> = Singleton(
    type = definition.type,
    lazy = definition.lazy,
    createInstance = state.instance,
    onShape = state.onShape
)
