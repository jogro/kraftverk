/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import io.kraftverk.*

const val ACTIVE_PROFILES = "kraftverk.active.profiles"

internal class Container(private val lazy: Boolean, val refreshable: Boolean, val environment: Environment) {

    @Volatile
    private var state: State = State.Defining()

    fun <T : Any> newValue(name: String, config: ValueConfig<T>) = ValueImpl(
        ValueDelegate(
            name = name,
            secret = config.secret,
            type = config.type,
            lazy = config.lazy ?: lazy,
            instance = {
                state.checkIsRunning()
                val value = environment[name] ?: config.default ?: throwValueNotFound(name)
                config.instance(ValueDefinition(environment), value)
            }
        )
    ).apply(::register)

    fun <T : Any> newBean(name: String, config: BeanConfig<T>) = BeanImpl(
        BeanDelegate(
            name = name,
            type = config.type,
            lazy = config.lazy ?: lazy,
            refreshable = config.refreshable ?: refreshable,
            instance = {
                state.checkIsRunning()
                config.instance(BeanDefinition(environment))
            }
        )
    ).apply(::register)

    fun start() {
        state.applyAs<State.Defining> {
            state = State.Running(bindings.toList())
            bindings.start()
            bindings.prepare()
        }
    }

    fun destroy() {
        state.applyWhen<State.Running> {
            state = State.Destroying
            bindings.destroy()
            state = State.Destroyed
        }
    }

    fun refresh() {
        state.applyAs<State.Running> {
            state = State.Refreshing
            bindings.beginRefresh()
            bindings.endRefresh()
            state = this
            bindings.prepare()
        }
    }

    private fun List<Binding<*>>.destroy() {
        filter { it.provider().instanceId != null }
            .sortedByDescending { it.provider().instanceId }
            .forEach { binding ->
                runCatching {
                    binding.destroy()
                }.onFailure { ex ->
                    ex.printStackTrace()
                }
            }
    }

    private fun List<Binding<*>>.start() {
        forEach { it.start() }
    }

    private fun List<Binding<*>>.beginRefresh() {
        forEach { it.beginRefresh() }
    }

    private fun List<Binding<*>>.endRefresh() {
        forEach { it.endRefresh() }
    }

    private fun List<Binding<*>>.prepare() {
        filterIsInstance<Value<*>>().forEach { it.prepare() }
        filterIsInstance<Bean<*>>().forEach { it.prepare() }
    }

    private fun register(binding: Binding<*>) {
        state.applyAs<State.Defining> {
            bindings.add(binding)
        }
    }

    private fun throwValueNotFound(name: String): Nothing =
        throw ValueNotFoundException("Value '$name' was not found!")

    private fun State.checkIsRunning() {
        narrow<State.Running>()
    }

    private sealed class State {

        data class Defining(val bindings: MutableList<Binding<*>> = mutableListOf()) : State()

        data class Running(val bindings: List<Binding<*>>) : State()

        object Refreshing : State()

        object Destroying : State()

        object Destroyed : State()
    }

}
