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

fun <M : Module> Modular.module(
    name: String? = null,
    instance: () -> M
): ModuleComponent<M> = object :
    ModuleComponent<M> {

    override fun provideDelegate(
        thisRef: Modular,
        property: KProperty<*>
    ): ReadOnlyProperty<Modular, M> {
        val moduleName = qualifyName(name ?: property.name)
        logger.debug { "Creating module '$moduleName'" }
        val module = createModule(moduleName, instance)
        return Delegate(module)
    }
}

fun <M : Modular, P : Partition<M>> M.partition(
    name: String? = null,
    instance: () -> P
): PartitionComponent<M, P> = object :
    PartitionComponent<M, P> {

    override fun provideDelegate(
        thisRef: Modular,
        property: KProperty<*>
    ): ReadOnlyProperty<Modular, P> {
        val moduleName = qualifyName(name ?: property.name)
        logger.debug { "Creating partition '$moduleName'" }
        val module = createPartition(moduleName, instance)
        return Delegate(module)
    }
}

fun <M : Modular, P : Partition<M>, T : Any, B : Bean<T>> P.ref(
    bean: M.() -> B
): BeanRefComponent<T> = object : BeanRefComponent<T> {
    override fun provideDelegate(
        thisRef: Modular,
        property: KProperty<*>
    ): ReadOnlyProperty<Modular, BeanRef<T>> {
        val moduleName = qualifyName(property.name)
        logger.debug { "Injecting bean '$moduleName'" }
        val beanRef = BeanRef { getInstance(bean) }
        return Delegate(beanRef)
    }
}

fun <M : Modular, P : Partition<M>, M1 : Modular> P.import(
    module: M.() -> M1
): ModuleRefComponent<M1> = object : ModuleRefComponent<M1> {
    override fun provideDelegate(
        thisRef: Modular,
        property: KProperty<*>
    ): ReadOnlyProperty<Modular, ModuleRef<M1>> {
        val moduleName = qualifyName(property.name)
        logger.debug { "Injecting module '$moduleName'" }
        val moduleRef = ModuleRef { root.module() }
        return Delegate(moduleRef)
    }
}
