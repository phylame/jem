package jclp.log

import java.util.logging.Level
import java.util.logging.Logger

private typealias LazyMessage = () -> String

object Log {
    var level = LogLevel.INFO

    var facade = SimpleFacade

    fun isEnable(level: LogLevel) = level.code <= Log.level.code

    inline fun t(tag: String, msg: LazyMessage) {
        if (isEnable(LogLevel.TRACE)) {
            SimpleFacade.log(tag, LogLevel.TRACE, msg())
        }
    }

    inline fun t(tag: String, t: Throwable, msg: LazyMessage) {
        if (isEnable(LogLevel.TRACE)) {
            SimpleFacade.log(tag, LogLevel.TRACE, msg(), t)
        }
    }

    inline fun d(tag: String, msg: LazyMessage) {
        if (isEnable(LogLevel.DEBUG)) {
            SimpleFacade.log(tag, LogLevel.DEBUG, msg())
        }
    }

    inline fun d(tag: String, t: Throwable, msg: LazyMessage) {
        if (isEnable(LogLevel.DEBUG)) {
            SimpleFacade.log(tag, LogLevel.DEBUG, msg(), t)
        }
    }

    inline fun i(tag: String, msg: LazyMessage) {
        if (isEnable(LogLevel.INFO)) {
            SimpleFacade.log(tag, LogLevel.INFO, msg())
        }
    }

    inline fun i(tag: String, t: Throwable, msg: LazyMessage) {
        if (isEnable(LogLevel.INFO)) {
            SimpleFacade.log(tag, LogLevel.INFO, msg(), t)
        }
    }

    inline fun w(tag: String, msg: LazyMessage) {
        if (isEnable(LogLevel.WARN)) {
            SimpleFacade.log(tag, LogLevel.WARN, msg())
        }
    }

    inline fun w(tag: String, t: Throwable, msg: LazyMessage) {
        if (isEnable(LogLevel.WARN)) {
            SimpleFacade.log(tag, LogLevel.WARN, msg(), t)
        }
    }

    inline fun e(tag: String, msg: LazyMessage) {
        if (isEnable(LogLevel.ERROR)) {
            SimpleFacade.log(tag, LogLevel.ERROR, msg())
        }
    }

    inline fun e(tag: String, t: Throwable, msg: LazyMessage) {
        if (isEnable(LogLevel.ERROR)) {
            SimpleFacade.log(tag, LogLevel.ERROR, msg(), t)
        }
    }
}

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

object SimpleFacade : LogFacade {
    override fun log(tag: String, level: LogLevel, msg: String) {
        print(tag, level, msg)
    }

    override fun log(tag: String, level: LogLevel, msg: String, t: Throwable) {
        print(tag, level, msg)
        t.printStackTrace()
    }

    private fun print(tag: String, level: LogLevel, msg: String) {
        val text = "[${Thread.currentThread().name}] ${level.name[0]}/$tag: $msg"
        if (level.code > LogLevel.WARN.code) {
            println(text)
        } else {
            System.err.println(text)
        }
    }
}

object JDKFacade : LogFacade {
    override fun log(tag: String, level: LogLevel, msg: String) {
        Logger.getLogger(tag).log(mapLevel(level), msg)
    }

    override fun log(tag: String, level: LogLevel, msg: String, t: Throwable) {
        Logger.getLogger(tag).log(mapLevel(level), msg, t)
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
