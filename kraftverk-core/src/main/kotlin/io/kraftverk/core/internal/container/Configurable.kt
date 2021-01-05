/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.core.internal.container

import io.kraftverk.core.binding.Bean
import io.kraftverk.core.binding.BeanImpl
import io.kraftverk.core.binding.Binding
import io.kraftverk.core.binding.Value
import io.kraftverk.core.binding.ValueImpl
import io.kraftverk.core.binding.delegate
import io.kraftverk.core.common.BeanDefinition
import io.kraftverk.core.common.ValueDefinition
import io.kraftverk.core.declaration.BeanDeclaration
import io.kraftverk.core.declaration.BeanDeclarationContext
import io.kraftverk.core.declaration.ValueDeclaration
import io.kraftverk.core.internal.container.Container.State
import io.kraftverk.core.internal.misc.applyAs
import io.kraftverk.core.module.Delegate
import kotlin.reflect.KClass

internal fun <T : Any> Container.createBean(
    name: String,
    type: KClass<T>,
    lazy: Boolean? = null,
    instance: BeanDeclaration.() -> T
): BeanImpl<T> {
    val ctx = BeanDeclarationContext()
    val definition = BeanDefinition(
        name = name,
        lazy = lazy,
        type = type,
        instance = { createBeanInstance(ctx, instance) }
    )
    return createBean(definition, ctx)
}

internal fun <T : Any> Container.createBean(
    definition: BeanDefinition<T>,
    ctx: BeanDeclarationContext
): BeanImpl<T> {
    state.applyAs<State.Configurable> {
        return beanFactory.createBean(definition, ctx).also { bindings.add(it) }
    }
}

internal fun <T : Any> Container.createValue(
    valueName: String,
    secret: Boolean,
    type: KClass<T>,
    default: T?,
    instance: ValueDeclaration.(Any) -> T
): Delegate<ValueImpl<T>> {
    val definition = ValueDefinition(
        name = valueName,
        lazy = null,
        secret = secret,
        type = type,
        instance = {
            createValueInstance(valueName, default, instance)
        }
    )
    return createValue(definition).let(::Delegate)
}

internal fun <T : Any> Container.createValue(
    definition: ValueDefinition<T>
): ValueImpl<T> {
    state.applyAs<State.Configurable> {
        return valueFactory.createValue(definition).also { bindings.add(it) }
    }
}

internal fun Container.configure(block: () -> Unit) =
    state.applyAs<State.Configurable> {
        val previous = onConfigure
        onConfigure = {
            previous()
            block()
        }
    }

internal fun Container.start(lazy: Boolean) =
    state.applyAs<State.Configurable> {
        onConfigure()
        bindings.forEach { binding -> binding.delegate.start() }
        state = State.Running(bindings.toList())
        bindings.initialize(lazy)
    }

private fun List<Binding<*>>.initialize(lazy: Boolean) {
    val valueNotFoundExceptions = mutableListOf<ValueNotFoundException>()
    filterIsInstance<Value<*>>().forEach { value ->
        try {
            value.delegate.initialize(lazy)
        } catch (e: ValueNotFoundException) {
            valueNotFoundExceptions += e
        }
    }
    valueNotFoundExceptions.takeIf { it.isNotEmpty() }
        ?.joinToString(separator = "\n") { "      - " + it.valueName }
        ?.let { errorMsg ->
            val exceptionMessage = """


Couldn't initialize the container since the following values seem to be missing:
$errorMsg

                """.trimIndent()
            throw IllegalStateException(exceptionMessage)
        }
    filterIsInstance<Bean<*>>().forEach { bean -> bean.delegate.initialize(lazy) }
}
