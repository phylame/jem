package jclp

import java.text.MessageFormat
import java.util.*

interface Translator {
    fun tr(key: String): String

    fun optTr(key: String, default: String) = try {
        tr(key) or default
    } catch (e: MissingResourceException) {
        default
    }

    fun tr(key: String, vararg args: Any) = MessageFormat.format(tr(key), *args)

    fun optTr(key: String, fallback: String, vararg args: Any) = MessageFormat.format(optTr(key, fallback), *args)
}
