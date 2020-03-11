package io.kraftverk.internal.binding

import io.kraftverk.internal.misc.Consumer
import io.kraftverk.internal.misc.InstanceFactory
import kotlin.reflect.KClass

internal data class BindingConfig<T : Any>(
    val name: String,
    val type: KClass<T>,
    val lazy: Boolean,
    val secret: Boolean,
    var instance: InstanceFactory<T>,
    var onCreate: Consumer<T> = { },
    var onDestroy: Consumer<T> = { }
)
