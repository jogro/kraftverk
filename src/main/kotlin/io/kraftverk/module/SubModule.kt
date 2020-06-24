/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Component
import io.kraftverk.common.ComponentRef
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <PARENT : BasicModule<*>, CHILD : BasicModule<PARENT>> PARENT.module(
    name: String? = null,
    instance: () -> CHILD
): ModuleDelegateProvider<PARENT, CHILD> = object :
    ModuleDelegateProvider<PARENT, CHILD> {

    override fun provideDelegate(
        thisRef: AbstractModule,
        property: KProperty<*>
    ): ReadOnlyProperty<AbstractModule, CHILD> {
        val moduleName = qualifyName(name ?: property.name)
        val module: CHILD = createChildModule(moduleName, instance)
        return Delegate(module)
    }
}

fun <PARENT : BasicModule<*>, CHILD : ChildModule<PARENT>, T : Any, COMPONENT : Component<T>> CHILD.ref(
    component: PARENT.() -> COMPONENT
): ComponentRefDelegateProvider<T> = object : ComponentRefDelegateProvider<T> {
    override fun provideDelegate(
        thisRef: AbstractModule,
        property: KProperty<*>
    ): ReadOnlyProperty<AbstractModule, ComponentRef<T>> {
        val ref = ComponentRef { getInstance(component) }
        return Delegate(ref)
    }
}
