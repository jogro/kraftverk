package io.kraftverk.internal.container

internal class ValueNotFoundException(message: String, val valueName: String) : Exception(message)