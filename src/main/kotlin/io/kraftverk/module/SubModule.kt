/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.common.BeanRef
import io.kraftverk.common.ModuleRef
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <AM : AbstractModule, CM : ChildModule<AM>> AM.module(
    name: String? = null,
    instance: () -> CM
): ModuleComponent<AM, CM> = object :
    ModuleComponent<AM, CM> {

    override fun provideDelegate(
        thisRef: AbstractModule,
        property: KProperty<*>
    ): ReadOnlyProperty<AbstractModule, CM> {
        val moduleName = qualifyName(name ?: property.name)
        val module = createChildModule(moduleName, instance)
        return Delegate(module)
    }
}

fun <AM : AbstractModule, CM : ChildModule<AM>, T : Any, B : Bean<T>> CM.ref(
    bean: AM.() -> B
): BeanRefComponent<T> = object : BeanRefComponent<T> {
    override fun provideDelegate(
        thisRef: AbstractModule,
        property: KProperty<*>
    ): ReadOnlyProperty<AbstractModule, BeanRef<T>> {
        val beanRef = BeanRef { getInstance(bean) }
        return Delegate(beanRef)
    }
}

fun <AM : AbstractModule, CM : ChildModule<AM>, AM1 : AbstractModule> CM.import(
    module: AM.() -> AM1
): ModuleRefComponent<AM1> = object : ModuleRefComponent<AM1> {
    override fun provideDelegate(
        thisRef: AbstractModule,
        property: KProperty<*>
    ): ReadOnlyProperty<AbstractModule, ModuleRef<AM1>> {
        val moduleRef = ModuleRef { parent.module() }
        return Delegate(moduleRef)
    }
}
