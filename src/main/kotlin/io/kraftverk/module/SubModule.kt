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

fun <AM : AbstractModule, MO : ModuleOf<AM>> AM.module(
    name: String? = null,
    instance: () -> MO
): ModuleComponent<AM, MO> = object :
    ModuleComponent<AM, MO> {

    override fun provideDelegate(
        thisRef: AbstractModule,
        property: KProperty<*>
    ): ReadOnlyProperty<AbstractModule, MO> {
        val moduleName = qualifyName(name ?: property.name)
        logger.debug { "Creating partition '$moduleName'" }
        val module = createModuleOf(moduleName, instance)
        return Delegate(module)
    }
}

fun <AM : AbstractModule, MO : ModuleOf<AM>, T : Any, B : Bean<T>> MO.ref(
    bean: AM.() -> B
): BeanRefComponent<T> = object : BeanRefComponent<T> {
    override fun provideDelegate(
        thisRef: AbstractModule,
        property: KProperty<*>
    ): ReadOnlyProperty<AbstractModule, BeanRef<T>> {
        val beanName = qualifyName(property.name)
        logger.debug { "Referencing bean '$beanName'" }
        val beanRef = BeanRef { getInstance(bean) }
        return Delegate(beanRef)
    }
}

fun <AM : AbstractModule, MO : ModuleOf<AM>, AM1 : AbstractModule> MO.import(
    module: AM.() -> AM1
): ModuleRefComponent<AM1> = object : ModuleRefComponent<AM1> {
    override fun provideDelegate(
        thisRef: AbstractModule,
        property: KProperty<*>
    ): ReadOnlyProperty<AbstractModule, ModuleRef<AM1>> {
        val moduleName = qualifyName(property.name)
        logger.debug { "Importing module '$moduleName'" }
        val moduleRef = ModuleRef { parent.module() }
        return Delegate(moduleRef)
    }
}
