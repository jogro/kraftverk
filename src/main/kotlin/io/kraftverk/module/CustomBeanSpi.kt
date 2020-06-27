package io.kraftverk.module

interface CustomBeanSpi<S> {
    fun onSetUp(setUp: (S) -> Unit)
}
