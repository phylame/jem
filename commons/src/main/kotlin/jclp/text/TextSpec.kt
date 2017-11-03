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

import java.io.Writer

const val TEXT_HTML = "html"
const val TEXT_PLAIN = "plain"

interface Text {
    val type get() = TEXT_PLAIN

    override fun toString(): String

    operator fun iterator() = toString().split("\r\n", "\n", "\r").iterator()

    fun writeTo(output: Writer) {
        output.write(toString())
    }
}

open class TextWrapper(val text: Text) : Text {
    override val type = text.type

    override fun toString() = text.toString()

    override fun iterator() = text.iterator()

    override fun writeTo(output: Writer) = text.writeTo(output)

    override fun equals(other: Any?) = text == other

    override fun hashCode() = text.hashCode()
}
