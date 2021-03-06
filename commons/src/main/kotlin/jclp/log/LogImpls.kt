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

import java.time.LocalDateTime
import java.util.logging.Level
import java.util.logging.Logger

object SimpleFacade : LogFacade {
    override var level: LogLevel = LogLevel.INFO

    override fun log(tag: String, level: LogLevel, msg: String) {
        print(tag, level, msg)
    }

    override fun log(tag: String, level: LogLevel, msg: String, t: Throwable) {
        print(tag, level, msg)
        t.printStackTrace()
    }

    private fun print(tag: String, level: LogLevel, msg: String) {
        val text = "[${LocalDateTime.now()}] [${Thread.currentThread().name}] ${level.name[0]}/$tag: $msg"
        if (level == LogLevel.ERROR) {
            System.err.println(text)
        } else {
            println(text)
        }
    }
}

object JDKFacade : LogFacade {
    override var level: LogLevel = LogLevel.INFO

    override fun log(tag: String, level: LogLevel, msg: String) {
        Logger.getLogger(tag).let {
            it.level = mapLevel(level)
            it.log(mapLevel(level), msg)
        }
    }

    override fun log(tag: String, level: LogLevel, msg: String, t: Throwable) {
        Logger.getLogger(tag).let {
            it.level = mapLevel(level)
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
