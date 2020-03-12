package io.kraftverk.internal.container

import io.kraftverk.binding.Bean
import io.kraftverk.binding.BeanImpl
import io.kraftverk.binding.Binding
import io.kraftverk.binding.Value
import io.kraftverk.binding.ValueImpl
import io.kraftverk.binding.handler
import io.kraftverk.internal.binding.BeanHandler
import io.kraftverk.internal.binding.BindingConfig
import io.kraftverk.internal.binding.ValueHandler
import io.kraftverk.internal.binding.initialize
import io.kraftverk.internal.binding.start
import io.kraftverk.internal.misc.applyAs

internal fun Container.register(binding: Binding<*>) =
    state.applyAs<Container.State.Defining> {
        bindings.add(binding)
    }

internal fun <T : Any> Container.createBean(
    config: BindingConfig<T>
): BeanImpl<T> =
    BeanHandler(config)
        .let(::BeanImpl)
        .also(this::register)

internal fun <T : Any> Container.createValue(
    config: BindingConfig<T>
): ValueImpl<T> = ValueHandler(config)
    .let(::ValueImpl)
    .also(this::register)

internal fun Container.start() = state.applyAs<Container.State.Defining> {
    bindings.start()
    state = Container.State.Started(bindings.toList())
    bindings.initialize()
}

private fun List<Binding<*>>.start() {
    forEach { binding ->
        binding.handler.start()
    }
}

private fun List<Binding<*>>.initialize() {
    filterIsInstance<Value<*>>().forEach { value ->
        value.handler.initialize()
    }
    filterIsInstance<Bean<*>>().forEach { bean ->
        bean.handler.initialize()
    }
}