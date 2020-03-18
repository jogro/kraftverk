package io.kraftverk.common

import io.kraftverk.module.Modular

class BeanRef<out T : Any>(internal val instance: () -> T)
class ModuleRef<out M : Modular>(internal val instance: () -> M)
