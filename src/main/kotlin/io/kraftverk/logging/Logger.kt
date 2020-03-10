/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.logging

import org.slf4j.Logger as Slf4jLogger
import org.slf4j.LoggerFactory

class Logger(private val logger: Slf4jLogger) {

    fun info(block: () -> String) {
        if (logger.isInfoEnabled) logger.info(block())
    }

    fun debug(block: () -> String) {
        if (logger.isDebugEnabled) logger.debug(block())
    }

    fun trace(block: () -> String) {
        if (logger.isTraceEnabled) logger.trace(block())
    }

    fun warn(block: () -> String) {
        if (logger.isWarnEnabled) logger.warn(block())
    }

    fun error(message: String, t: Throwable) {
        if (logger.isErrorEnabled) logger.error(message, t)
    }
}

fun newLogger(block: () -> Unit) = Logger(LoggerFactory.getLogger(name(block)))

private fun name(func: () -> Unit): String =
    func.javaClass.name.run {
        when {
            contains("Kt$") -> substringBefore("Kt$")
            contains("$") -> substringBefore("$")
            else -> this
        }
    }
