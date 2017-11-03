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

import jclp.DisposableSupport
import jclp.flob.Flob
import jclp.io.LineIterator
import jclp.tryRelease
import jclp.tryRetain
import java.io.Writer
import java.nio.charset.Charset

private class StringText(override val type: String, private val text: CharSequence) : Text {
    init {
        require(type.isNotEmpty()) { "type cannot be empty" }
    }

    override fun toString() = text.toString()
}

fun textOf(cs: CharSequence, type: String = TEXT_PLAIN): Text = StringText(type, cs)

fun emptyText(type: String = TEXT_PLAIN) = textOf("", type)

private class FlobText(override val type: String, val flob: Flob, encoding: String? = null) : DisposableSupport(), Text {
    private val charset: Charset = when {
        encoding.isNullOrEmpty() -> Charset.defaultCharset()
        else -> Charset.forName(encoding)
    }

    init {
        require(type.isNotEmpty()) { "type cannot be empty" }
        flob.tryRetain()
    }

    override fun toString() = openReader().use { it.readText() }

    override fun iterator() = LineIterator(openReader(), true)

    override fun writeTo(output: Writer) {
        openReader().use { it.copyTo(output) }
    }

    private fun openReader() = flob.openStream().bufferedReader(charset)

    override fun dispose() {
        flob.tryRelease()
    }
}

fun textOf(source: Flob, encoding: String? = null, type: String = TEXT_PLAIN): Text = FlobText(type, source, encoding)
