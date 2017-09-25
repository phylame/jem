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

package jclp.vdm

import jclp.ServiceManager
import jclp.ServiceProvider
import jclp.flob.Flob
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

fun VDMReader.openStream(name: String) = getEntry(name)?.let(this::getInputStream)

fun VDMReader.readText(name: String, charset: Charset = Charsets.UTF_8): String? {
    return openStream(name)?.use { it.reader(charset).readText() }
}

interface VDMWriter : Closeable {
    fun setComment(comment: String)

    fun setProperty(name: String, value: Any)

    fun newEntry(name: String): VDMEntry

    fun putEntry(entry: VDMEntry): OutputStream

    fun closeEntry(entry: VDMEntry)
}

inline fun <R> VDMWriter.useStream(name: String, block: (OutputStream) -> R): R = newEntry(name).let {
    try {
        block(putEntry(it))
    } finally {
        closeEntry(it)
    }
}

fun VDMWriter.write(name: String, flob: Flob) = useStream(name) {
    flob.writeTo(it)
    it.flush()
}

fun VDMWriter.write(name: String, text: Text, charset: Charset = Charsets.UTF_8) = useStream(name) {
    it.writer(charset).let { writer ->
        text.writeTo(writer)
        writer.flush()
    }
}

interface VDMFactory : ServiceProvider {
    fun getReader(input: Any): VDMReader

    fun getWriter(output: Any): VDMWriter
}

object VDMManager : ServiceManager<VDMFactory>(VDMFactory::class.java) {
    fun openReader(name: String, input: Any) = get(name)?.getReader(input)

    fun openWriter(name: String, output: Any) = get(name)?.getWriter(output)
}
