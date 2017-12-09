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

const val ISO_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"

const val ANSIC_DATE_TIME_FORMAT = "EEE MMM d HH:mm:ss z yyyy"

const val RFC1036_DATE_TIME_FORMAT = "EEEEEE, dd-MMM-yy HH:mm:ss z"

const val UTC_DATE_TIME_FORMAT = "$ISO_DATE_TIME_FORMAT'Z'"

const val LOOSE_DATE_TIME_FORMAT = "yyyy-M-d H:m:s"

const val LOOSE_DATE_FORMAT = "yyyy-M-d"

const val LOOSE_TIME_FORMAT = "H:m:s"

val looseISODate: DateTimeFormatter by lazy { DateTimeFormatter.ofPattern(LOOSE_DATE_FORMAT) }

val looseISOTime: DateTimeFormatter by lazy { DateTimeFormatter.ofPattern(LOOSE_TIME_FORMAT) }

val looseISODateTime: DateTimeFormatter by lazy { DateTimeFormatter.ofPattern(LOOSE_DATE_TIME_FORMAT) }

val utcISODateTime: DateTimeFormatter by lazy { DateTimeFormatter.ofPattern(UTC_DATE_TIME_FORMAT) }

fun parseDate(text: String, vararg patterns: String): Date? {
    for (pattern in patterns) {
        try {
            return SimpleDateFormat(pattern).parse(text)
        } catch (ignored: ParseException) {
        }
    }
    return null
}

fun detectDate(text: String): Date? =
        parseDate(text, LOOSE_DATE_TIME_FORMAT, LOOSE_DATE_FORMAT, LOOSE_TIME_FORMAT)

fun Date.format(pattern: String): String = SimpleDateFormat(pattern).format(this)

fun String.toLocalDate(formatter: DateTimeFormatter = looseISODate): LocalDate =
        LocalDate.parse(this, formatter)

fun String.toLocalTime(formatter: DateTimeFormatter = looseISOTime): LocalTime =
        LocalTime.parse(this, formatter)

fun String.toLocalDateTime(formatter: DateTimeFormatter = looseISODateTime): LocalDateTime =
        LocalDateTime.parse(this, formatter)
