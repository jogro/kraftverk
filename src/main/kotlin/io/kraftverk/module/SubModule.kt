/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.BeanRef
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <M : Module> ModuleSupport.module(
    name: String? = null,
    instance: () -> M
): ModuleComponent<M> = object :
    ModuleComponent<M> {

    override fun provideDelegate(
        thisRef: ModuleSupport,
        property: KProperty<*>
    ): ReadOnlyProperty<ModuleSupport, M> {
        val moduleName = qualifyName(name ?: property.name)
        logger.debug { "Creating module '$moduleName'" }
        val module = createModule(moduleName, instance)
        return Delegate(module)
    }
}

fun <M : ModuleSupport, P : PartitionOf<M>> M.partition(
    name: String? = null,
    instance: () -> P
): PartitionComponent<M, P> = object :
    PartitionComponent<M, P> {

    override fun provideDelegate(
        thisRef: ModuleSupport,
        property: KProperty<*>
    ): ReadOnlyProperty<ModuleSupport, P> {
        val moduleName = qualifyName(name ?: property.name)
        logger.debug { "Creating partition '$moduleName'" }
        val module = createPartition(moduleName, instance)
        return Delegate(module)
    }
}

fun <M : ModuleSupport, P : PartitionOf<M>, T : Any, B : Bean<T>> P.inject(
    bean: M.() -> B
): BeanRefComponent<T> = object : BeanRefComponent<T> {
    override fun provideDelegate(
        thisRef: ModuleSupport,
        property: KProperty<*>
    ): ReadOnlyProperty<ModuleSupport, BeanRef<T>> {
        val moduleName = qualifyName(property.name)
        logger.debug { "Injecting bean '$moduleName'" }
        val beanRef = BeanRef { getInstance(bean) }
        return Delegate(beanRef)
    }
}
