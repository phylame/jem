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

import jclp.Reusable
import jclp.ReusableHelper
import jclp.io.clippedStream
import jclp.io.copyRange
import jclp.io.detectMime
import jclp.io.getResource
import jclp.releaseSelf
import jclp.retainSelf
import jclp.vdm.VDMReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

abstract class AbstractFlob(private val type: String) : Flob {
    override val mime get() = detectMime(name, type)

    override fun toString() = "$name;mime=$mime"
}

internal class URLFlob(private val url: URL, mime: String) : AbstractFlob(mime) {
    override val name: String = url.path

    override fun openStream(): InputStream = url.openStream()

    override fun toString() = "$url;mime=$mime"
}

fun flobOf(url: URL, mime: String = ""): Flob = URLFlob(url, mime)

fun flobOf(uri: String, loader: ClassLoader? = null, mime: String = ""): Flob {
    return flobOf(getResource(uri, loader) ?: throw IllegalArgumentException("No such resource: '$uri'"), mime)
}

internal class PathFlob(private val path: Path, mime: String) : AbstractFlob(mime) {
    init {
        require(Files.isRegularFile(path)) { "$path is not file" }
    }

    override val name: String = path.toString()

    override fun openStream(): InputStream = Files.newInputStream(path)

    override fun toString() = "file://${name.replace('\\', '/')}"
}

fun flobOf(path: Path, mime: String = ""): Flob = PathFlob(path, mime)

internal class ByteFlob(override val name: String, private val data: ByteArray, mime: String) : AbstractFlob(mime) {
    override fun openStream() = ByteArrayInputStream(data)

    override fun writeTo(output: OutputStream) = data.let {
        output.write(it)
        it.size.toLong()
    }

    override fun toString() = "bytes://${super.toString()}"
}

fun flobOf(name: String, data: ByteArray, mime: String): Flob = ByteFlob(name, data, mime)

fun emptyFlob(mime: String = "") = flobOf("_empty_", ByteArray(0), mime)

internal class BlockFlob(
        override val name: String,
        val file: RandomAccessFile,
        private val offset: Long,
        private val length: Long,
        mime: String
) : AbstractFlob(mime), Reusable {
    init {
        file.retainSelf()
        require(offset + length <= file.length()) { "offset($offset) + length($length) > total(${file.length()})" }
    }

    override fun openStream() = file.let {
        it.seek(offset)
        it.clippedStream(offset, length)
    }

    override fun writeTo(output: OutputStream) = file.let {
        it.seek(offset)
        it.copyRange(output, length)
    }

    private val helper = object : ReusableHelper() {
        override fun dispose() = file.releaseSelf()
    }

    override fun release() = helper.release()

    override fun retain() = helper.retain()

    override fun toString() = "clip://${super.toString()}"
}

fun flobOf(name: String, file: RandomAccessFile, offset: Long, length: Long, mime: String = ""): Flob {
    return BlockFlob(name, file, offset, length, mime)
}

internal class VDMFlob(val reader: VDMReader, override val name: String, mime: String) : AbstractFlob(mime), Reusable {
    init {
        reader.retainSelf()
    }

    private val entry = reader.getEntry(name) ?: throw IllegalArgumentException("No such entry '$name'")

    override fun openStream() = reader.getInputStream(entry)

    private val helper = object : ReusableHelper() {
        override fun dispose() = reader.releaseSelf()
    }

    override fun release() = helper.release()

    override fun retain() = helper.retain()

    override fun toString() = "$entry;mime=$mime"
}

fun flobOf(reader: VDMReader, name: String, mime: String = ""): Flob = VDMFlob(reader, name, mime)
