/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.common

import io.kraftverk.module.AbstractModule

class BeanRef<out T : Any>(internal val instance: () -> T)
class ComponentRef<out T : Any>(internal val instance: () -> T)
class ModuleRef<out M : AbstractModule>(internal val instance: () -> M)
