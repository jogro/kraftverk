package io.kraftverk.binding

class BeanRef<out T : Any>(internal val instance: () -> T)
