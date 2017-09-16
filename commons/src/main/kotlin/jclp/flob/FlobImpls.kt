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

import jclp.io.clippedStream
import jclp.io.copyLimited
import jclp.io.detectMime
import jclp.vdm.VDMEntry
import jclp.vdm.VDMReader
import java.io.*
import java.net.URL

abstract class AbstractFlob(private val _mime: String) : Flob {
    override val mime get() = detectMime(name, _mime)

    override fun toString() = "$name;mime=$mime"
}

internal class URLFlob(private val url: URL, mime: String) : AbstractFlob(mime) {
    override val name: String = url.path

    override fun openStream(): InputStream = url.openStream()

    override fun toString() = "$url;mime=$mime"
}

internal class FileFlob(private val file: File, mime: String) : AbstractFlob(mime) {
    init {
        if (!file.isFile) {
            throw NoSuchFileException(file)
        }
    }

    override val name: String = file.path

    override fun openStream() = file.inputStream()

    override fun toString() = "file:/${name.replace('\\', '/')}"
}

internal class ByteFlob(override val name: String, private val data: ByteArray, mime: String) : AbstractFlob(mime) {
    override fun openStream() = ByteArrayInputStream(data)

    override fun writeTo(output: OutputStream) = data.let {
        output.write(it)
        it.size.toLong()
    }

    override fun toString() = "bytes://${super.toString()}"
}

internal class BlockFlob(
        override val name: String,
        private val file: RandomAccessFile,
        private val offset: Long,
        private val length: Long,
        mime: String
) : AbstractFlob(mime) {
    init {
        require(offset + length <= file.length()) { "offset($offset) + length($length) > total(${file.length()})" }
    }

    override fun openStream() = file.let {
        it.seek(offset)
        it.clippedStream(offset, length)
    }

    override fun writeTo(output: OutputStream) = file.let {
        it.seek(offset)
        it.copyLimited(output, length)
    }
}

internal class VDMFlob(private val reader: VDMReader, private val entry: VDMEntry, mime: String) : AbstractFlob(mime) {
    override val name = entry.name

    override fun openStream() = reader.getInputStream(entry)

    override fun toString() = "$entry;mime=$mime"
}
