/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import mu.KotlinLogging
import kotlin.reflect.KClass
import kotlin.time.measureTimedValue

const val ACTIVE_PROFILES = "kraftverk.active.profiles"

internal class Container(
    val lazy: Boolean,
    val refreshable: Boolean,
    val environment: Environment
) {

    @Volatile
    internal var state: State = State.Defining()

    internal sealed class State {

        class Defining : State() {
            val bindings = mutableListOf<Binding<*>>()
        }

        class Running(val bindings: List<Binding<*>>) : State()

        object Refreshing : State()

        object Destroying : State()

        object Destroyed : State()
    }

}

private val logger = KotlinLogging.logger {}

internal fun <T : Any> Container.newBean(
    name: String,
    config: BeanConfig<T>
) = BeanImpl(
    newBeanHandler(
        name = name,
        type = config.type,
        lazy = config.lazy ?: lazy,
        resettable = config.refreshable ?: refreshable,
        createInstance = {
            state.checkIsRunning()
            config.instance(BeanDefinition(environment))
        }
    )
).apply(::register)

internal fun <T : Any> Container.newValue(
    name: String,
    config: ValueConfig<T>
) = ValueImpl(
    newValueHandler(
        name = name,
        secret = config.secret,
        type = config.type,
        lazy = config.lazy ?: lazy,
        createInstance = {
            state.checkIsRunning()
            config.instance(
                ValueDefinition(environment),
                environment[name] ?: config.default ?: throwValueNotFound(name)
            )
        }
    )
).apply(::register)

internal fun Container.start() {
    state.applyAs<Container.State.Defining> {
        state = Container.State.Running(bindings.toList())
        bindings.start()
        bindings.initialize()
    }
}

internal fun Container.destroy() {
    state.applyWhen<Container.State.Running> {
        state = Container.State.Destroying
        bindings.destroy()
        state = Container.State.Destroyed
    }
}

internal fun Container.refresh() {
    state.applyAs<Container.State.Running> {
        state = Container.State.Refreshing
        bindings.reset()
        state = this
        bindings.initialize()
    }
}

private fun Container.State.checkIsRunning() {
    narrow<Container.State.Running>()
}

private fun Container.register(binding: Binding<*>) {
    state.applyAs<Container.State.Defining> {
        bindings.add(binding)
    }
}

private fun <T : Any> newBeanHandler(
    name: String,
    type: KClass<T>,
    lazy: Boolean,
    resettable: Boolean,
    createInstance: () -> T
): BindingHandler<T> {
    return BindingHandler(
        createInstance,
        createProvider = { create, onCreate, onDestroy ->
            Provider(
                type = type,
                lazy = lazy,
                resettable = resettable,
                create = {
                    measureTimedValue {
                        create()
                    }.also {
                        logger.info("Bean '$name' is bound to $type (${it.duration})")
                    }.value
                },
                onCreate = onCreate,
                onDestroy = onDestroy
            )
        }
    )
}

private fun <T : Any> newValueHandler(
    name: String,
    type: KClass<T>,
    lazy: Boolean,
    secret: Boolean,
    createInstance: () -> T
): BindingHandler<T> {

    return BindingHandler(
        createInstance,
        createProvider = { create, onCreate, onDestroy ->
            Provider(
                type = type,
                lazy = lazy,
                resettable = true,
                create = {
                    create().also {
                        if (secret) {
                            logger.info("Value '$name' is bound to '********'")
                        } else {
                            logger.info("Value '$name' is bound to '$it'")
                        }
                    }
                },
                onCreate = onCreate,
                onDestroy = onDestroy
            )
        }
    )
}


private fun List<Binding<*>>.destroy() {
    filter { it.provider.instanceId != null }
        .sortedByDescending { it.provider.instanceId }
        .forEach { it.destroy() }
}

private fun List<Binding<*>>.start() {
    forEach { it.start() }
}

private fun List<Binding<*>>.reset() {
    forEach { it.reset() }
}

private fun List<Binding<*>>.initialize() {
    filterIsInstance<Value<*>>().forEach { it.initialize() }
    filterIsInstance<Bean<*>>().forEach { it.initialize() }
}

private fun throwValueNotFound(name: String): Nothing =
    throw IllegalStateException("Value '$name' was not found!")

