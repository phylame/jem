package jclp

import java.text.MessageFormat
import java.util.logging.Level
import java.util.logging.Logger


object Log {
    var level = LogLevel.INFO

    var facade = SimpleFacade

    fun isEnable(level: LogLevel) = level.code <= this.level.code

    fun t(tag: String, msg: String) {
        log(tag, LogLevel.TRACE, msg)
    }

    fun t(tag: String, format: String, vararg args: Any) {
        log(tag, LogLevel.TRACE, format, *args)
    }

    fun t(tag: String, msg: String, t: Throwable) {
        log(tag, LogLevel.TRACE, msg, t)
    }

    fun d(tag: String, msg: String) {
        log(tag, LogLevel.DEBUG, msg)
    }

    fun d(tag: String, format: String, vararg args: Any) {
        log(tag, LogLevel.DEBUG, format, *args)
    }

    fun d(tag: String, msg: String, t: Throwable) {
        log(tag, LogLevel.DEBUG, msg, t)
    }

    fun i(tag: String, msg: String) {
        log(tag, LogLevel.INFO, msg)
    }

    fun i(tag: String, format: String, vararg args: Any) {
        log(tag, LogLevel.INFO, format, *args)
    }

    fun i(tag: String, msg: String, t: Throwable) {
        log(tag, LogLevel.INFO, msg, t)
    }

    fun w(tag: String, msg: String) {
        log(tag, LogLevel.WARN, msg)
    }

    fun w(tag: String, format: String, vararg args: Any) {
        log(tag, LogLevel.WARN, format, *args)
    }

    fun w(tag: String, msg: String, t: Throwable) {
        log(tag, LogLevel.WARN, msg, t)
    }

    fun e(tag: String, msg: String) {
        log(tag, LogLevel.ERROR, msg)
    }

    fun e(tag: String, format: String, vararg args: Any) {
        log(tag, LogLevel.ERROR, format, *args)
    }

    fun e(tag: String, msg: String, t: Throwable) {
        log(tag, LogLevel.ERROR, msg, t)
    }

    private fun log(tag: String, level: LogLevel, msg: String) {
        if (isEnable(level)) {
            facade.log(tag, level, msg)
        }
    }

    private fun log(tag: String, level: LogLevel, format: String, vararg args: Any) {
        if (isEnable(level)) {
            facade.log(tag, level, MessageFormat.format(format, *args))
        }
    }

    private fun log(tag: String, level: LogLevel, msg: String, t: Throwable) {
        if (isEnable(level)) {
            facade.log(tag, level, msg, t)
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
        else -> null
    }
}
