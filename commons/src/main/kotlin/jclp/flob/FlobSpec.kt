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

package jclp.flob

import jclp.io.getResource
import jclp.vdm.VDMReader
import java.io.*
import java.net.URL
import java.nio.file.NoSuchFileException

interface Flob {
    val name: String

    val mime: String

    fun openStream(): InputStream

    fun writeTo(output: OutputStream) = openStream().use { it.copyTo(output) }

    companion object {
        fun of(url: URL, mime: String = ""): Flob = URLFlob(url, mime)

        fun of(file: File, mime: String = ""): Flob = FileFlob(file, mime)

        fun of(data: ByteArray, name: String, mime: String = ""): Flob = ByteFlob(name, data, mime)

        fun of(file: RandomAccessFile, name: String, offset: Long, length: Long, mime: String = ""): Flob {
            return BlockFlob(name, file, offset, length, mime)
        }

        fun of(uri: String, loader: ClassLoader? = null, mime: String = ""): Flob = getResource(uri, loader)?.let {
            URLFlob(it, mime)
        } ?: throw IOException("No such resource: $uri")

        fun of(reader: VDMReader, name: String, mime: String = ""): Flob = reader.getEntry(name)?.let {
            VDMFlob(reader, it, mime)
        } ?: throw NoSuchFileException("No such file '$name' in '${reader.name}'")

        fun empty(mime: String = "") = of(ByteArray(0), "_empty_", mime)
    }
}

open class FlobWrapper(private val flob: Flob) : Flob {
    override val name = flob.name

    override val mime = flob.mime

    override fun openStream() = flob.openStream()

    override fun writeTo(output: OutputStream) = flob.writeTo(output)

    override fun equals(other: Any?) = flob == other

    override fun hashCode() = super.hashCode()

    override fun toString() = flob.toString()
}
