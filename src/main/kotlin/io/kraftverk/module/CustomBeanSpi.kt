package io.kraftverk.module

interface CustomBeanSpi<S> {
    fun onConfigure(configure: (S) -> Unit)
}
