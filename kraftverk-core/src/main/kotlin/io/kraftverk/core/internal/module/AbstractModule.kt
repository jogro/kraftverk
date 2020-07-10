package io.kraftverk.core.internal.module

import io.kraftverk.core.internal.container.Container
import io.kraftverk.core.internal.logging.createLogger

abstract class AbstractModule {

    internal val logger = createLogger { }
    internal abstract val container: Container
    internal abstract val namespace: String

    fun qualifyMemberName(name: String) = if (namespace.isBlank()) name else "$namespace.$name"
}
