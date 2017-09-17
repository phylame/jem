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

import jclp.flob.Flob
import java.io.Reader
import java.io.Writer
import java.nio.charset.Charset

abstract class AbstractText(final override val type: String) : Text {
    init {
        require(type.isNotEmpty()) { "type cannot be empty" }
    }
}

internal class StringText(type: String, private val text: CharSequence) : AbstractText(type) {
    override fun toString() = text.toString()
}

internal class FlobText(type: String, private val flob: Flob, encoding: String? = null) : AbstractText(type) {
    private val charset: Charset = when {
        encoding == null -> Charset.defaultCharset()
        encoding.isNotEmpty() -> Charset.forName(encoding)
        else -> throw IllegalArgumentException("Unsupported encoding: $encoding")
    }

    override fun toString() = openReader().use(Reader::readText)

    override fun iterator() = openReader().useLines { it.toList().iterator() }

    override fun writeTo(output: Writer) = openReader().use { it.copyTo(output) }

    private fun openReader() = flob.openStream().bufferedReader(charset)
}
