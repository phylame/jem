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

package jclp.text

import java.util.*

infix fun CharSequence?.or(default: CharSequence) = if (!isNullOrEmpty()) toString() else default.toString()

infix inline fun CharSequence?.or(default: () -> CharSequence) = if (!isNullOrEmpty()) toString() else default().toString()

fun String.remove(text: CharSequence) = if (text.isEmpty()) this else replaceFirst(text.toString(), "")

fun String.valueFor(name: String, partSeparator: String = ";", valueSeparator: String = "="): String? {
    split(partSeparator).forEach {
        val parts = it.trim().split(valueSeparator)
        if (parts.first().trim() == name) {
            return if (parts.size > 1) parts[1].trim() else null
        }
    }
    return null
}

class LineSplitter(private val text: String) : Iterator<String> {
    private val length = text.length

    private var from = 0
    private var begin = 0
    private var end = length

    private var found = false

    override fun hasNext(): Boolean {
        if (!found) {
            begin = from
            if (begin != end) {
                found = true
            }
            var i = from
            var j = from
            while (i < length) {
                val c = text[i]
                if (c == '\n') {
                    j = i++
                    break
                } else if (c == '\r') {
                    j = i
                    if (i < length - 1 && text[i + 1] == '\n') {
                        i += 2
                    } else {
                        i++
                    }
                    break
                } else {
                    j = i + 1
                }
                ++i
            }
            from = i
            end = j
        }
        return found
    }

    override fun next(): String {
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        val line = text.substring(begin, end)
        found = false
        return line
    }
}
