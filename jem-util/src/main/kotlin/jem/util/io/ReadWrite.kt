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

package jem.util.io

import java.io.BufferedReader
import java.io.Writer
import java.util.*

fun Writer.writeLines(lines: Iterable<String>, separator: String = System.lineSeparator()) {
    val it = lines.iterator()
    while (it.hasNext()) {
        write(it.next())
        if (it.hasNext()) {
            write(separator)
        }
    }
}

class LineIterator(private val reader: BufferedReader, private val autoClose: Boolean = false) : Iterator<String> {
    private var isDone = false
    private var nextLine: String? = null

    override fun hasNext(): Boolean {
        if (nextLine == null && !isDone) {
            nextLine = reader.readLine()
            if (nextLine == null) {
                isDone = true
                if (autoClose) {
                    reader.close()
                }
            }
        }
        return nextLine != null
    }

    override fun next(): String {
        if (!hasNext()) throw NoSuchElementException()
        val line = nextLine!!
        nextLine = null
        return line
    }
}
