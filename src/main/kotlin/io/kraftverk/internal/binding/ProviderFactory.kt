package io.kraftverk.internal.binding

import io.kraftverk.common.BindingDefinition
import io.kraftverk.common.ComponentDefinition
import io.kraftverk.common.ValueDefinition
import io.kraftverk.declaration.LifecycleActions
import io.kraftverk.internal.logging.createLogger
import io.kraftverk.internal.misc.Supplier
import io.kraftverk.internal.misc.interceptAfter
import io.kraftverk.internal.misc.interceptAround
import io.kraftverk.internal.provider.Singleton
import io.kraftverk.provider.ComponentProvider
import io.kraftverk.provider.ComponentProviderImpl
import io.kraftverk.provider.Provider
import io.kraftverk.provider.ValueProvider
import io.kraftverk.provider.ValueProviderImpl

private val logger = createLogger { }

internal abstract class BindingProviderFactory<T : Any, out P : Provider<T>>(var instance: Supplier<T>) {

    abstract fun createProvider(): P

    fun bind(block: (Supplier<T>) -> T) {
        instance = interceptAround(instance, block)
    }
}

internal class ComponentProviderFactory<T : Any, S : Any>(
    private val definition: ComponentDefinition<T, S>
) : BindingProviderFactory<T, ComponentProvider<T, S>>(definition.instance) {

    private var onConfigure: (T, LifecycleActions) -> Unit = { _, _ -> }

    fun configure(block: (T, LifecycleActions) -> Unit) {
        onConfigure = interceptAfter(onConfigure, block)
    }

    override fun createProvider() = ComponentProviderImpl(
        definition,
        createSingleton(
            definition,
            instance = loggingInterceptor(instance),
            onConfigure = onConfigure
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

internal class ValueProviderFactory<T : Any>(
    private val definition: ValueDefinition<T>
) : BindingProviderFactory<T, ValueProvider<T>>(definition.instance) {

    override fun createProvider() = ValueProviderImpl(
        definition,
        createSingleton(
            definition,
            instance = loggingInterceptor(instance),
            onConfigure = { _, _ -> }
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

private fun <T : Any> createSingleton(
    definition: BindingDefinition<T>,
    instance: Supplier<T>,
    onConfigure: (T, LifecycleActions) -> Unit

): Singleton<T> = Singleton(
    type = definition.type,
    lazy = definition.lazy,
    createInstance = instance,
    onConfigure = onConfigure
)
