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

package jclp.text

operator fun String.get(start: Int, end: Int) = substring(start, end)

infix fun CharSequence?.or(default: CharSequence) = if (this != null && isNotEmpty()) toString() else default.toString()

infix inline fun CharSequence?.or(default: () -> CharSequence) = if (this != null && isNotEmpty()) toString() else default().toString()

fun String.valueFor(name: String, partSeparator: String = ";", valueSeparator: String = "="): String? {
    split(partSeparator).map(String::trim).forEach {
        val parts = it.split(valueSeparator)
        if (parts.first().trim() == name) {
            return if (parts.size > 1) parts[1].trim() else null
        }
    }
    return null
}
