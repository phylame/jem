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

package jem.util.text

import jem.util.DisposableSupport
import jem.util.io.Flob
import jem.util.io.LineIterator
import jem.util.io.writeLines
import jem.util.release
import jem.util.retain
import java.io.Writer
import java.nio.charset.Charset

const val TEXT_HTML = "html"
const val TEXT_PLAIN = "plain"

interface Text : Iterable<String> {
    val type: String

    override fun toString(): String

    override fun iterator(): Iterator<String> {
        return LineSplitter(toString())
    }

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

private class StringText(val text: CharSequence, override val type: String) : Text {
    override fun toString() = text.toString()
}

fun textOf(text: CharSequence, type: String = TEXT_PLAIN): Text {
    require(type.isNotEmpty()) { "'type' cannot be empty" }
    return StringText(text, type)
}

fun emptyText(type: String = TEXT_PLAIN) = textOf("", type)

abstract class IteratorText(final override val type: String) : Text {
    init {
        require(type.isNotEmpty()) { "'type' cannot be empty" }
    }

    abstract override fun iterator(): Iterator<String>

    override fun toString(): String {
        return joinToString(System.lineSeparator())
    }

    override fun writeTo(output: Writer) {
        output.writeLines(this)
    }
}

fun textOf(iterator: Iterator<String>, type: String = TEXT_PLAIN) = object : IteratorText(type) {
    override fun iterator() = iterator
}

private class FlobText(val flob: Flob, val charset: Charset, override val type: String) : DisposableSupport(), Text {
    init {
        flob.retain()
    }

    override fun toString() = openReader().use { it.readText() }

    override fun iterator() = LineIterator(openReader(), true)

    fun openReader() = flob.openStream().bufferedReader(charset)

    override fun writeTo(output: Writer) {
        openReader().use { it.copyTo(output) }
    }

    override fun dispose() {
        flob.release()
    }
}

fun textOf(flob: Flob, charset: Charset? = null, type: String = TEXT_PLAIN): Text {
    require(type.isNotEmpty()) { "'type' cannot be empty" }
    return FlobText(flob, charset ?: Charset.defaultCharset(), type)
}
