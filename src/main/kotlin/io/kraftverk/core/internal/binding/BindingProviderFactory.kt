package io.kraftverk.core.internal.binding

import io.kraftverk.core.common.BeanDefinition
import io.kraftverk.core.common.BindingDefinition
import io.kraftverk.core.common.ValueDefinition
import io.kraftverk.core.declaration.LifecycleActions
import io.kraftverk.core.internal.logging.createLogger
import io.kraftverk.core.internal.misc.Supplier
import io.kraftverk.core.internal.misc.interceptAfter
import io.kraftverk.core.internal.misc.interceptAround
import io.kraftverk.core.internal.provider.Singleton
import io.kraftverk.core.provider.BeanProvider
import io.kraftverk.core.provider.BeanProviderImpl
import io.kraftverk.core.provider.Provider
import io.kraftverk.core.provider.ValueProvider
import io.kraftverk.core.provider.ValueProviderImpl

private val logger = createLogger { }

internal sealed class BindingProviderFactory<T : Any, out P : Provider<T>>(var instance: Supplier<T>) {
    abstract fun createProvider(): P
}

internal class BeanProviderFactory<T : Any>(
    private val definition: BeanDefinition<T>,
    private val lifecycleActions: LifecycleActions
) : BindingProviderFactory<T, BeanProvider<T>>(definition.instance) {

    private var onConfigure: (T) -> Unit = { }

    fun bind(block: (LifecycleActions, Supplier<T>) -> T) {
        instance = interceptAround(instance) { proceed ->
            block(lifecycleActions, proceed)
        }
    }

    fun configure(block: (T, LifecycleActions) -> Unit) {
        onConfigure = interceptAfter(onConfigure) { t ->
            block(t, lifecycleActions)
        }
    }

    override fun createProvider() = BeanProviderImpl(
        definition,
        createSingleton(
            definition,
            instance = loggingInterceptor(instance),
            onConfigure = onConfigure,
            lifecycleActions = lifecycleActions
        )
    )

    private fun loggingInterceptor(block: () -> T): () -> T = {
        val startMs = System.currentTimeMillis()
        val t = block()
        val elapsed = System.currentTimeMillis() - startMs
        logger.info {
            "Bean '${definition.name}' is bound to ${definition.type} (${elapsed}ms)"
        }
        t
    }
}

internal class ValueProviderFactory<T : Any>(
    private val definition: ValueDefinition<T>
) : BindingProviderFactory<T, ValueProvider<T>>(definition.instance) {

    fun bind(block: (Supplier<T>) -> T) {
        instance = interceptAround(instance, block)
    }

    override fun createProvider() = ValueProviderImpl(
        definition,
        createSingleton(
            definition,
            instance = loggingInterceptor(instance),
            onConfigure = { },
            lifecycleActions = LifecycleActions()
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
    lifecycleActions: LifecycleActions,
    onConfigure: (T) -> Unit

): Singleton<T> =
    Singleton(
        type = definition.type,
        lazy = definition.lazy,
        createInstance = instance,
        onConfigure = onConfigure,
        lifecycleActions = lifecycleActions
    )
