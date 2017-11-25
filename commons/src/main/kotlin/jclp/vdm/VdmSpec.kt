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

interface VdmEntry {
    val name: String

    val comment: String?

    val lastModified: Long

    val isDirectory: Boolean
}

interface VdmReader : Closeable {
    val name: String

    val comment: String?

    fun getEntry(name: String): VdmEntry?

    fun getInputStream(entry: VdmEntry): InputStream

    val entries: Iterator<VdmEntry>

    val size: Int
}

fun VdmReader.openStream(name: String) = getEntry(name)?.let { getInputStream(it) }

fun VdmReader.readBytes(name: String, estimatedSize: Int = DEFAULT_BUFFER_SIZE): ByteArray? =
        openStream(name)?.use { it.readBytes(estimatedSize) }

fun VdmReader.readText(name: String, charset: Charset = Charsets.UTF_8): String? =
        openStream(name)?.use { it.reader(charset).readText() }

interface VdmWriter : Closeable {
    fun setComment(comment: String)

    fun newEntry(name: String): VdmEntry

    fun putEntry(entry: VdmEntry): OutputStream

    fun closeEntry(entry: VdmEntry)
}

inline fun <R> VdmWriter.useStream(name: String, block: (OutputStream) -> R): R {
    return with(newEntry(name)) {
        val stream = putEntry(this)
        try {
            block(stream)
        } finally {
            closeEntry(this)
        }
    }
}

fun VdmWriter.writeBytes(name: String, data: ByteArray) = useStream(name) {
    it.write(data)
}

fun VdmWriter.writeFlob(name: String, flob: Flob) = useStream(name) {
    flob.writeTo(it)
    it.flush()
}

fun VdmWriter.writeText(name: String, text: Text, charset: Charset = Charsets.UTF_8) = useStream(name) {
    with(it.writer(charset)) {
        text.writeTo(this)
        flush()
    }
}

interface VdmFactory : ServiceProvider {
    fun getReader(input: Any, props: VariantMap): VdmReader

    fun getWriter(output: Any, props: VariantMap): VdmWriter
}

object VdmManager : ServiceManager<VdmFactory>(VdmFactory::class.java) {
    fun openReader(name: String, input: Any, props: VariantMap = emptyMap()): VdmReader? =
            get(name)?.getReader(input, props)

    fun openWriter(name: String, output: Any, props: VariantMap = emptyMap()): VdmWriter? =
            get(name)?.getWriter(output, props)
}
