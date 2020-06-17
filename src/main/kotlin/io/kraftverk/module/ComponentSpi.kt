package io.kraftverk.module

interface ComponentSpi<S> {
    fun onShape(shape: (S) -> Unit)
}
