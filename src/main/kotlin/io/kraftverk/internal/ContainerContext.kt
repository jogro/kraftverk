/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.*
import kotlin.reflect.KClass

const val ACTIVE_PROFILES = "kraftverk.active.profiles"

internal class ContainerContext private constructor(lazy: Boolean, propertyValues: PropertyValues) {

    private var state: State = State.Defining(
        BeanFactory(propertyValues.profiles, lazy),
        PropertyFactory(propertyValues, lazy)
    )

    val profiles: List<String> = propertyValues.profiles

    fun <T : Any> newBean(
        type: KClass<T>,
        lazy: Boolean? = null,
        namespace: String,
        instance: BeanDefinition.() -> T
    ): DelegateProvider<Module, Bean<T>> {
        state.applyAs<State.Defining> {
            return beanFactory.newBean(
                type,
                lazy,
                namespace,
                instance
            )
        }
    }

    fun <T : Any> newProperty(
        type: KClass<T>,
        name: String?,
        default: String?,
        lazy: Boolean?,
        secret: Boolean,
        namespace: String,
        instance: PropertyDefinition.(String) -> T
    ): DelegateProvider<Module, Property<T>> {
        state.applyAs<State.Defining> {
            return propertyFactory.newProperty(
                type,
                name,
                default,
                lazy,
                secret,
                namespace,
                instance
            )
        }
    }

    fun start() {
        state.applyAs<State.Defining> {
            state = State.Running(
                propertyFactory.properties.toList(),
                beanFactory.beans.toList()
            ).apply {
                properties.forEach { it.initialize() }
                beans.forEach { it.initialize() }
                properties.forEach { it.evaluate() }
                beans.forEach { it.evaluate() }
            }
        }
    }

    fun destroy() {
        state.applyWhen<State.Running> {
            beans.filter { it.provider().instanceId != null }
                .sortedByDescending { it.provider().instanceId }
                .forEach { bean ->
                    runCatching {
                        bean.destroy()
                    }.onFailure { ex ->
                        ex.printStackTrace()
                    }
                }
            state = State.Destroyed
        }

    }

    private sealed class State {

        data class Defining(val beanFactory: BeanFactory, val propertyFactory: PropertyFactory) : State()

        data class Running(val properties: List<Property<*>>, val beans: List<Bean<*>>) : State()

        object Destroyed : State()
    }

    companion object {
        fun create(lazy: Boolean, propertySource: (List<String>) -> PropertySource): ContainerContext {
            val propertyValues = PropertyValues.create(propertySource)
            return ContainerContext(lazy, propertyValues)
        }
    }

}
