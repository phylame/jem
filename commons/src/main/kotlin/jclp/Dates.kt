/*
 * Copyright 2017 Peng Wan <phylame@163.com>
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
import java.time.format.DateTimeFormatter
import java.util.*

const val ISO_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"

const val ANSIC_DATE_FORMAT = "EEE MMM d HH:mm:ss z yyyy"

const val RFC1123_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z"

const val RFC1036_DATE_FORMAT = "EEEEEE, dd-MMM-yy HH:mm:ss z"

const val LOOSE_TIME_FORMAT = "H:m:s"

const val LOOSE_DATE_FORMAT = "yyyy-M-d"

const val LOOSE_DATE_TIME_FORMAT = "yyyy-M-d H:m:s"

val LOOSE_ISO_DATE: DateTimeFormatter by lazy { DateTimeFormatter.ofPattern(LOOSE_DATE_FORMAT) }

val LOOSE_ISO_TIME: DateTimeFormatter by lazy { DateTimeFormatter.ofPattern(LOOSE_TIME_FORMAT) }

val LOOSE_ISO_DATE_TIME: DateTimeFormatter by lazy { DateTimeFormatter.ofPattern(LOOSE_DATE_TIME_FORMAT) }

fun parseDate(text: String, vararg patterns: String): Date? {
    for (pattern in patterns) {
        try {
            SimpleDateFormat(pattern).parse(text)
        } catch (ignored: ParseException) {
        }
    }
    return null
}

fun detectDate(text: String) = parseDate(text, LOOSE_DATE_TIME_FORMAT, LOOSE_DATE_FORMAT, LOOSE_TIME_FORMAT)

fun Date.format(pattern: String): String = SimpleDateFormat(pattern).format(this)
