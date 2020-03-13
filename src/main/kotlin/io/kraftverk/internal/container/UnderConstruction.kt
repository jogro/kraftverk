package io.kraftverk.internal.container

import io.kraftverk.binding.Bean
import io.kraftverk.binding.BeanImpl
import io.kraftverk.binding.Binding
import io.kraftverk.binding.Value
import io.kraftverk.binding.ValueImpl
import io.kraftverk.binding.handler
import io.kraftverk.internal.binding.BeanConfig
import io.kraftverk.internal.binding.BeanHandler
import io.kraftverk.internal.binding.ValueConfig
import io.kraftverk.internal.binding.ValueHandler
import io.kraftverk.internal.binding.initialize
import io.kraftverk.internal.binding.start
import io.kraftverk.internal.container.Container.State
import io.kraftverk.internal.misc.mustBe

internal fun <T : Any> Container.createBean(
    config: BeanConfig<T>
): BeanImpl<T> = BeanHandler(config)
    .let(::BeanImpl)
    .also(this::register)

internal fun <T : Any> Container.createValue(
    config: ValueConfig<T>
): ValueImpl<T> = ValueHandler(config)
    .let(::ValueImpl)
    .also(this::register)

internal fun Container.register(binding: Binding<*>) =
    state.mustBe<State.UnderConstruction> {
        bindings.add(binding)
    }

internal fun Container.start() =
    state.mustBe<State.UnderConstruction> {
        bindings.start()
        state = State.Running(bindings.toList())
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
