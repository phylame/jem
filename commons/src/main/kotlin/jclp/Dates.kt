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

package jclp

import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

const val ISO_FORMAT = "yyyy-MM-dd HH:mm:ss"

const val ANSIC_FORMAT = "EEE MMM d HH:mm:ss z yyyy"

const val RFC1123_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z"

const val RFC1036_FORMAT = "EEEEEE, dd-MMM-yy HH:mm:ss z"

const val DATE_TIME_FORMAT = "yyyy-M-d H:m:s"

const val DATE_FORMAT = "yyyy-M-d"

const val TIME_FORMAT = "H:m:s"

val looseISODate: DateTimeFormatter by lazy { DateTimeFormatter.ofPattern(DATE_FORMAT) }

val looseISOTime: DateTimeFormatter by lazy { DateTimeFormatter.ofPattern(TIME_FORMAT) }

val looseISODateTime: DateTimeFormatter by lazy { DateTimeFormatter.ofPattern(DATE_TIME_FORMAT) }

fun parseDate(text: String, vararg patterns: String): Date? {
    for (pattern in patterns) {
        try {
            SimpleDateFormat(pattern).parse(text)
        } catch (ignored: ParseException) {
        }
    }
    return null
}

fun Date.format(pattern: String): String = SimpleDateFormat(pattern).format(this)

fun detectDate(text: String): Date? =
        parseDate(text, DATE_TIME_FORMAT, DATE_FORMAT, TIME_FORMAT)

fun String.toLocalDate(formatter: DateTimeFormatter = looseISODate): LocalDate =
        LocalDate.parse(this, formatter)

fun String.toLocalTime(formatter: DateTimeFormatter = looseISOTime): LocalTime =
        LocalTime.parse(this, formatter)

fun String.toLocalDateTime(formatter: DateTimeFormatter = looseISODateTime): LocalDateTime =
        LocalDateTime.parse(this, formatter)
