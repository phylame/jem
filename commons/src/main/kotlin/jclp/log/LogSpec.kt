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

private typealias LazyMessage = () -> String

enum class LogLevel(val code: Int) {
    ALL(6),
    TRACE(5),
    DEBUG(4),
    INFO(3),
    WARN(2),
    ERROR(1),
    OFF(0);
}

interface LogFacade {
    fun log(tag: String, level: LogLevel, msg: String)

    fun log(tag: String, level: LogLevel, msg: String, t: Throwable)
}

object Log {
    var level: LogLevel = LogLevel.INFO

    var facade: LogFacade = SimpleFacade

    fun isEnable(level: LogLevel) = level.code <= this.level.code

    inline fun t(tag: String, msg: LazyMessage) {
        if (isEnable(LogLevel.TRACE)) {
            facade.log(tag, LogLevel.TRACE, msg())
        }
    }

    inline fun t(tag: String, t: Throwable, msg: LazyMessage) {
        if (isEnable(LogLevel.TRACE)) {
            facade.log(tag, LogLevel.TRACE, msg(), t)
        }
    }

    inline fun d(tag: String, msg: LazyMessage) {
        if (isEnable(LogLevel.DEBUG)) {
            facade.log(tag, LogLevel.DEBUG, msg())
        }
    }

    inline fun d(tag: String, t: Throwable, msg: LazyMessage) {
        if (isEnable(LogLevel.DEBUG)) {
            facade.log(tag, LogLevel.DEBUG, msg(), t)
        }
    }

    inline fun i(tag: String, msg: LazyMessage) {
        if (isEnable(LogLevel.INFO)) {
            facade.log(tag, LogLevel.INFO, msg())
        }
    }

    inline fun i(tag: String, t: Throwable, msg: LazyMessage) {
        if (isEnable(LogLevel.INFO)) {
            facade.log(tag, LogLevel.INFO, msg(), t)
        }
    }

    inline fun w(tag: String, msg: LazyMessage) {
        if (isEnable(LogLevel.WARN)) {
            facade.log(tag, LogLevel.WARN, msg())
        }
    }

    inline fun w(tag: String, t: Throwable, msg: LazyMessage) {
        if (isEnable(LogLevel.WARN)) {
            facade.log(tag, LogLevel.WARN, msg(), t)
        }
    }

    inline fun e(tag: String, msg: LazyMessage) {
        if (isEnable(LogLevel.ERROR)) {
            facade.log(tag, LogLevel.ERROR, msg())
        }
    }

    inline fun e(tag: String, t: Throwable, msg: LazyMessage) {
        if (isEnable(LogLevel.ERROR)) {
            facade.log(tag, LogLevel.ERROR, msg(), t)
        }
    }
}
