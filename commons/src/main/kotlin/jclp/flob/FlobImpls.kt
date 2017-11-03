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

package jclp.flob

import jclp.DisposableSupport
import jclp.io.clippedStream
import jclp.io.copyRange
import jclp.io.detectMime
import jclp.io.getResource
import jclp.tryRelease
import jclp.tryRetain
import jclp.vdm.VDMReader
import java.io.*
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

private class URLFlob(private val url: URL, mime: String) : Flob {
    override val name: String = url.path

    override val mimeType = detectMime(name, mime)

    override fun openStream(): InputStream = url.openStream()

    override fun toString() = "$url;mime=$mimeType"
}

fun flobOf(url: URL, mime: String = ""): Flob = URLFlob(url, mime)

fun flobOf(uri: String, loader: ClassLoader? = null, mime: String = ""): Flob {
    return flobOf(getResource(uri, loader) ?: throw IOException("No such resource: '$uri'"), mime)
}

private class PathFlob(private val path: Path, mime: String) : Flob {
    init {
        require(Files.isRegularFile(path)) { "$path is not regular file" }
    }

    override val name: String = path.toString()

    override val mimeType = detectMime(name, mime)

    override fun openStream(): InputStream = Files.newInputStream(path)

    override fun toString() = "${path.toUri()};mime=$mimeType"
}

fun flobOf(path: Path, mime: String = ""): Flob = PathFlob(path, mime)

fun flobOf(file: File, mime: String = ""): Flob = PathFlob(file.toPath(), mime)

private class ByteFlob(override val name: String, private val data: ByteArray, mime: String) : Flob {
    override val mimeType = detectMime(name, mime)

    override fun openStream() = ByteArrayInputStream(data)

    override fun writeTo(output: OutputStream) = with(data) {
        output.write(this)
        size.toLong()
    }

    override fun toString() = "bytes://$name;mime=$mimeType"
}

fun flobOf(name: String, data: ByteArray, mime: String): Flob = ByteFlob(name, data, mime)

fun emptyFlob(mime: String = "") = flobOf("_empty_", ByteArray(0), mime)

private class BlockFlob(
        override val name: String,
        val file: RandomAccessFile,
        private val offset: Long,
        private val length: Long,
        mime: String
) : DisposableSupport(), Flob {
    override val mimeType: String = detectMime(name, mime)

    init {
        require(offset + length <= file.length()) { "offset($offset) + length($length) > total(${file.length()})" }
        file.tryRetain()
    }

    override fun openStream() = with(file) {
        seek(offset)
        clippedStream(offset, length)
    }

    override fun writeTo(output: OutputStream) = with(file) {
        seek(offset)
        copyRange(output, length)
    }

    override fun toString() = "clip://$name;mime=$mimeType"

    override fun dispose() {
        file.tryRelease()
    }
}

fun flobOf(name: String, file: RandomAccessFile, offset: Long, length: Long, mime: String = ""): Flob {
    return BlockFlob(name, file, offset, length, mime)
}

private class VDMFlob(val reader: VDMReader, override val name: String, mime: String) : DisposableSupport(), Flob {
    private val entry = reader.getEntry(name) ?: throw IOException("No such entry '$name'")

    init {
        reader.tryRetain()
    }

    override val mimeType: String = detectMime(name, mime)

    override fun openStream() = reader.getInputStream(entry)

    override fun toString() = "$entry;mime=$mimeType"

    override fun dispose() {
        reader.tryRelease()
    }
}

fun flobOf(reader: VDMReader, name: String, mime: String = ""): Flob = VDMFlob(reader, name, mime)
