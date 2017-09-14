package jclp

import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*

const val ISO_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"

const val ANSIC_DATE_FORMAT = "EEE MMM d HH:mm:ss z yyyy"

const val RFC1123_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z"

const val RFC1036_DATE_FORMAT = "EEEEEE, dd-MMM-yy HH:mm:ss z"

const val LOOSE_ISO_TIME_FORMAT = "H:m:s"

const val LOOSE_ISO_DATE_FORMAT = "yyyy-M-d"

const val LOOSE_ISO_DATE_TIME_FORMAT = "yyyy-M-d H:m:s"

val LOOSE_ISO_DATE by lazy { DateTimeFormatter.ofPattern(LOOSE_ISO_DATE_FORMAT) }

val LOOSE_ISO_TIME by lazy { DateTimeFormatter.ofPattern(LOOSE_ISO_TIME_FORMAT) }

val LOOSE_ISO_DATE_TIME by lazy { DateTimeFormatter.ofPattern(LOOSE_ISO_DATE_TIME_FORMAT) }

fun Date.format(pattern: String): String = SimpleDateFormat(pattern).format(this)

fun String.toDate(pattern: String, default: Date? = null) = try {
    SimpleDateFormat(pattern).parse(this)
} catch (e: ParseException) {
    default
}

fun String.toDate(vararg patterns: String): Date? {
    for (pattern in patterns) {
        return SimpleDateFormat(pattern).parse(this) ?: continue
    }
    return null
}
