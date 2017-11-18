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

package jem.util.io

import jem.util.DisposableSupport
import jem.util.release
import jem.util.retain
import jem.util.text.or
import jem.util.vdm.VDMEntry
import jem.util.vdm.VDMReader
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

interface Flob {
    val name: String

    val mimeType: String

    fun openStream(): InputStream

    fun writeTo(output: OutputStream) {
        openStream().use { it.copyTo(output) }
    }
}

open class FlobWrapper(val flob: Flob) : Flob {
    override val name = flob.name

    override val mimeType = flob.mimeType

    override fun openStream() = flob.openStream()

    override fun writeTo(output: OutputStream) = flob.writeTo(output)

    override fun equals(other: Any?) = flob == other

    override fun hashCode() = flob.hashCode()

    override fun toString() = flob.toString()
}

private class URLFlob(val url: URL, mime: String) : Flob {
    override val name = fullName(url.path)

    override val mimeType = mime or { mimeType(name) }

    override fun openStream(): InputStream = url.openStream()

    override fun toString() = "$url;mime=$mimeType"
}

fun flobOf(url: URL, mime: String = ""): Flob = URLFlob(url, mime)

fun flobOf(uri: String, loader: ClassLoader? = null, mime: String = ""): Flob =
        getResource(uri, loader)?.let { flobOf(it, mime) } ?: throw IOException("No such resource: $uri")

private class FileFlob(val path: Path, mime: String) : Flob {
    override val name = path.fileName.toString()

    override val mimeType = mime or { mimeType(name) }

    override fun openStream(): InputStream = Files.newInputStream(path)

    override fun writeTo(output: OutputStream) {
        Files.copy(path, output)
    }

    override fun toString() = "$path;mime=$mimeType"
}

fun flobOf(path: Path, mime: String = ""): Flob = FileFlob(path, mime)

private class ByteFlob(val data: ByteArray, override val name: String, mime: String) : Flob {
    override val mimeType = mime or { mimeType(name) }

    override fun openStream(): InputStream = data.inputStream()

    override fun writeTo(output: OutputStream) = output.write(data)

    override fun toString() = "byte://$name;mime=$mimeType"
}

fun flobOf(name: String, data: ByteArray, mime: String): Flob = ByteFlob(data, name, mime)

fun emptyFlob(name: String = "_empty_", mime: String = "") = flobOf(name, ByteArray(0), mime)

private class VDMFlob(val reader: VDMReader, val entry: VDMEntry, mime: String) : DisposableSupport(), Flob {
    init {
        reader.retain()
    }

    override val name = entry.name

    override val mimeType = mime or { mimeType(name) }

    override fun openStream() = reader.getInputStream(entry)

    override fun toString() = "$entry;mime=$mimeType"

    override fun dispose() {
        reader.release()
    }
}

fun flobOf(reader: VDMReader, name: String, mime: String = ""): Flob {
    val entry = reader.getEntry(name) ?: throw IOException("No such entry: $name")
    return VDMFlob(reader, entry, mime)
}
