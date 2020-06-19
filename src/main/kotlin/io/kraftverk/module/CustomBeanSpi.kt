package io.kraftverk.module

interface CustomBeanSpi<S> {
    fun onShape(shape: (S) -> Unit)
}
