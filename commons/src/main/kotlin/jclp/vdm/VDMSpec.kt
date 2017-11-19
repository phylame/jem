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

package jclp.vdm

import jclp.ServiceManager
import jclp.ServiceProvider
import jclp.VariantMap
import jclp.io.Flob
import jclp.text.Text
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

interface VDMEntry {
    val name: String

    val comment: String?

    val lastModified: Long

    val isDirectory: Boolean
}

interface VDMReader : Closeable {
    val name: String

    val comment: String?

    fun getEntry(name: String): VDMEntry?

    fun getInputStream(entry: VDMEntry): InputStream

    val entries: Iterator<VDMEntry>

    val size: Int
}

fun VDMReader.openStream(name: String) = getEntry(name)?.let { getInputStream(it) }

fun VDMReader.readBytes(name: String): ByteArray? = openStream(name)?.use { it.readBytes() }

fun VDMReader.readText(name: String, charset: Charset = Charsets.UTF_8): String? =
        openStream(name)?.use { it.reader(charset).readText() }

interface VDMWriter : Closeable {
    fun setComment(comment: String)

    fun newEntry(name: String): VDMEntry

    fun putEntry(entry: VDMEntry): OutputStream

    fun closeEntry(entry: VDMEntry)
}

inline fun <R> VDMWriter.useStream(name: String, block: (OutputStream) -> R): R {
    return with(newEntry(name)) {
        val stream = putEntry(this)
        try {
            block(stream)
        } finally {
            closeEntry(this)
        }
    }
}

fun VDMWriter.write(name: String, flob: Flob) = useStream(name) {
    flob.writeTo(it)
    it.flush()
}

fun VDMWriter.write(name: String, text: Text, charset: Charset = Charsets.UTF_8) = useStream(name) {
    with(it.writer(charset)) {
        text.writeTo(this)
        flush()
    }
}

interface VDMFactory : ServiceProvider {
    fun getReader(input: Any, props: VariantMap): VDMReader

    fun getWriter(output: Any, props: VariantMap): VDMWriter
}

object VDMManager : ServiceManager<VDMFactory>(VDMFactory::class.java) {
    fun openReader(name: String, input: Any, props: VariantMap = emptyMap()): VDMReader? =
            get(name)?.getReader(input, props)

    fun openWriter(name: String, output: Any, props: VariantMap = emptyMap()): VDMWriter? =
            get(name)?.getWriter(output, props)
}
