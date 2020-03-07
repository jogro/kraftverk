/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.component

import io.kraftverk.binding.Bean
import io.kraftverk.binding.Value
import io.kraftverk.component.BeanComponent
import io.kraftverk.component.SubModuleComponent
import io.kraftverk.component.ValueComponent
import io.kraftverk.internal.container.newBean
import io.kraftverk.internal.container.newValue
import io.kraftverk.internal.module.BasicModule
import io.kraftverk.internal.module.createSubModule
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@PublishedApi
internal fun <T : Any> newBeanComponent(
    config: BeanConfig<T>
): BeanComponent<T> = object : BeanComponent<T> {

    override fun provideDelegate(
        thisRef: BasicModule,
        property: KProperty<*>
    ): ReadOnlyProperty<BasicModule, Bean<T>> {
        val beanName = property.name.toQualifiedName(thisRef)
        val bean = thisRef.container.newBean(beanName, config)
        return Delegate(bean)
    }
}

@PublishedApi
internal fun <T : Any> newValueComponent(
    name: String?,
    config: ValueConfig<T>
): ValueComponent<T> = object : ValueComponent<T> {

    override fun provideDelegate(
        thisRef: BasicModule,
        property: KProperty<*>
    ): ReadOnlyProperty<BasicModule, Value<T>> {
        val valueName = (name ?: property.name).toQualifiedName(thisRef).toSpinalCase()
        val value = thisRef.container.newValue(valueName, config)
        return Delegate(value)
    }
}

internal fun <M : BasicModule> newSubModuleComponent(
    name: String? = null,
    subModule: () -> M
): SubModuleComponent<M> = object : SubModuleComponent<M> {

    override fun provideDelegate(
        thisRef: BasicModule,
        property: KProperty<*>
    ): ReadOnlyProperty<BasicModule, M> {
        val moduleName = (name ?: property.name).toQualifiedName(thisRef)
        val module = createSubModule(moduleName) { subModule() }
        return Delegate(module)
    }
}

private class Delegate<T : Any>(private val t: T) :
    ReadOnlyProperty<BasicModule, T> {
    override fun getValue(thisRef: BasicModule, property: KProperty<*>): T {
        return t
    }
}

private fun String.toQualifiedName(module: BasicModule) =
    (if (module.namespace.isBlank()) this else "${module.namespace}.$this")

private val spinalRegex = "([A-Z]+)".toRegex()

private fun String.toSpinalCase() = replace(spinalRegex, "\\-$1").toLowerCase()
