/*
 * Copyright 2015-2017 Peng Wan <phylame@163.com>
 *
 * This file is part of Jem.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jclp.log

import org.joor.Reflect
import org.joor.Reflect.on
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.logging.Level
import java.util.logging.Logger

object SimpleFacade : LogFacade {
    override fun log(tag: String, level: LogLevel, msg: String) {
        print(tag, level, msg)
    }

    override fun log(tag: String, level: LogLevel, msg: String, t: Throwable) {
        print(tag, level, msg)
        t.printStackTrace()
    }

    private fun makeColor(reflect: Reflect, text: String, color: String): String =
            reflect.call("render", "@|$color $text|@").get()

    private fun print(tag: String, level: LogLevel, msg: String) {
        val text = "[${LocalDateTime.now()}] [${Thread.currentThread().name}] ${level.name[0]}/$tag: $msg"
        val reflect = on("org.fusesource.jansi.Ansi").call("ansi")
        when (level) {
            LogLevel.TRACE -> println(text)
            LogLevel.DEBUG -> println(makeColor(reflect, text, "green"))
            LogLevel.INFO -> println(makeColor(reflect, text, "yellow"))
            LogLevel.WARN -> println(makeColor(reflect, text, "magenta"))
            LogLevel.ERROR -> System.err.println(makeColor(reflect, text, "red"))
        }
    }
}

object JDKFacade : LogFacade {
    override fun log(tag: String, level: LogLevel, msg: String) {
        Logger.getLogger(tag).let {
            it.level = mapLevel(Log.level)
            it.log(mapLevel(level), msg)
        }
    }

    override fun log(tag: String, level: LogLevel, msg: String, t: Throwable) {
        Logger.getLogger(tag).let {
            it.level = mapLevel(Log.level)
            it.log(mapLevel(level), msg, t)
        }
    }

    private fun mapLevel(level: LogLevel) = when (level) {
        LogLevel.ALL -> Level.ALL
        LogLevel.TRACE -> Level.FINER
        LogLevel.DEBUG -> Level.FINE
        LogLevel.INFO -> Level.INFO
        LogLevel.WARN -> Level.WARNING
        LogLevel.ERROR -> Level.SEVERE
        LogLevel.OFF -> Level.OFF
    }
}

object SLF4JFacade : LogFacade {
    override fun log(tag: String, level: LogLevel, msg: String) {
        val logger = LoggerFactory.getLogger(tag)
        when (level) {
            LogLevel.DEBUG -> logger.debug(msg)
            LogLevel.TRACE -> logger.trace(msg)
            LogLevel.ERROR -> logger.error(msg)
            LogLevel.INFO -> logger.info(msg)
            LogLevel.WARN -> logger.warn(msg)
            else -> Unit
        }
    }

    override fun log(tag: String, level: LogLevel, msg: String, t: Throwable) {
        val logger = LoggerFactory.getLogger(tag)
        when (level) {
            LogLevel.DEBUG -> logger.debug(msg, t)
            LogLevel.TRACE -> logger.trace(msg, t)
            LogLevel.ERROR -> logger.error(msg, t)
            LogLevel.INFO -> logger.info(msg, t)
            LogLevel.WARN -> logger.warn(msg, t)
            else -> Unit
        }
    }
}
