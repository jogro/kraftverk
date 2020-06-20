/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.common

import io.kraftverk.internal.misc.Supplier
import kotlin.reflect.KClass

/**
 * Represents the outcome of a component or value declaration.
 */
interface BindingDefinition<T : Any> {
    val name: String
    val type: KClass<T>
    val lazy: Boolean?
    val instance: Supplier<T>
}

data class ValueDefinition<T : Any>(
    override val name: String,
    override val type: KClass<T>,
    override val lazy: Boolean?,
    val secret: Boolean,
    override val instance: Supplier<T>
) : BindingDefinition<T>

data class ComponentDefinition<T : Any, S : Any>(
    override val name: String,
    override val type: KClass<T>,
    override val lazy: Boolean?,
    val onConfigure: (T, (S) -> Unit) -> Unit,
    override val instance: Supplier<T>
) : BindingDefinition<T>

interface ComponentProcessor {
    fun <T : Any, S : Any> process(component: ComponentDefinition<T, S>): ComponentDefinition<T, S>
}

interface ValueProcessor {
    fun <T : Any> process(value: ValueDefinition<T>): ValueDefinition<T>
}
