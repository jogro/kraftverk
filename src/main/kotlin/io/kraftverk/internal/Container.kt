/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.*

const val ACTIVE_PROFILES = "kraftverk.active.profiles"

internal class Container(private val lazy: Boolean, private val environment: Environment) {

    private var state: State = State.Defining()

    val profiles: List<String> = environment.profiles

    fun <T : Any> newProperty(name: String, config: PropertyConfig<T>) = PropertyImpl(
        PropertyDelegate(
            name = name,
            secret = config.secret,
            type = config.type,
            lazy = config.lazy ?: lazy,
            instance = {
                val value = environment[name] ?: config.default
                ?: throw PropertyNotFoundException("Property '$name' was not found!")
                config.instance(PropertyDefinition(environment.profiles), value)
            }
        )
    ).apply(::register)


    fun <T : Any> newBean(name: String, config: BeanConfig<T>) = BeanImpl(
        BeanDelegate(
            name = name,
            type = config.type,
            lazy = config.lazy ?: lazy,
            instance = {
                config.instance(BeanDefinition(environment.profiles))
            }
        )
    ).apply(::register)


    fun start() {
        state.applyAs<State.Defining> {
            state = State.Running(bindings.toList()).apply {
                bindings.start()
                bindings.prepare()
            }
        }
    }

    fun refresh() {
        state.applyWhen<State.Running> {
            bindings.destroy(keepRunning = true)
            bindings.prepare()
        }
    }

    fun destroy() {
        state.applyWhen<State.Running> {
            bindings.destroy()
            state = State.Destroyed
        }
    }

    private fun List<Binding<*>>.destroy(keepRunning: Boolean = false) {
        filter { it.provider().instanceId != null }
            .sortedByDescending { it.provider().instanceId }
            .forEach { binding ->
                runCatching {
                    binding.destroy(keepRunning)
                }.onFailure { ex ->
                    ex.printStackTrace()
                }
            }
    }

    private fun List<Binding<*>>.start() {
        forEach { it.start() }
    }

    private fun List<Binding<*>>.prepare() {
        filterIsInstance<Property<*>>().forEach { it.prepare() }
        filterIsInstance<Bean<*>>().forEach { it.prepare() }
    }

    private fun register(binding: Binding<*>) {
        state.applyAs<State.Defining> {
            bindings.add(binding)
        }
    }

    private sealed class State {

        data class Defining(val bindings: MutableList<Binding<*>> = mutableListOf()) : State()

        data class Running(val bindings: List<Binding<*>>) : State()

        object Destroyed : State()
    }

}
