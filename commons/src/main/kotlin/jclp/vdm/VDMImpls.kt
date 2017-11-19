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

import jclp.DisposableSupport
import jclp.VariantMap
import jclp.io.slashify
import java.io.*
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.NotDirectoryException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

const val VDM_ZIP = "zip"
const val VDM_DIRECTORY = "dir"

private class FileVDMEntry(
        val path: Path, name: String, val reader: FileVDMReader? = null, val writer: FileVDMWriter? = null
) : VDMEntry {
    override val comment = null

    override val isDirectory = Files.isDirectory(path)

    override val lastModified
        get() = if (Files.exists(path)) Files.getLastModifiedTime(path).toMillis() else -1

    override val name: String = name + if (isDirectory) "/" else ""

    override fun toString(): String = path.toUri().toString()

    var stream: OutputStream? = null
}


private class FileVDMReader(val root: Path) : DisposableSupport(), VDMReader {
    override val comment = null

    override val name = root.toString()

    private val streams = LinkedList<InputStream>()

    override fun getEntry(name: String): FileVDMEntry? {
        require(name.isNotEmpty()) { "name for entry cannot be empty" }
        return root.resolve(name).takeIf { Files.isRegularFile(it) }?.let {
            FileVDMEntry(it, name.slashify(), this)
        }
    }

    override fun getInputStream(entry: VDMEntry): InputStream =
            if (entry !is FileVDMEntry || entry.reader != this) {
                throw IllegalArgumentException("Invalid entry: $entry")
            } else {
                Files.newInputStream(entry.path).also { streams += it }
            }

    override val entries: Iterator<VDMEntry>
        get() {
            val begin = root.toString().length + 1
            return walkRoot().map {
                FileVDMEntry(it, it.toString().substring(begin).slashify(), this)
            }.iterator()
        }

    override val size get() = walkRoot().count().toInt()

    private fun walkRoot() = Files.walk(root).filter { it !== root }

    override fun toString() = "FileVDMReader@${hashCode()}{root=$root}"

    override fun close() {
        streams.onEach { it.close() }.clear()
    }

    override fun dispose() {
        close()
    }

    fun finalize() {
        close()
    }
}

private class FileVDMWriter(val root: Path) : VDMWriter {
    private val streams = LinkedList<OutputStream>()

    override fun setComment(comment: String) {}

    override fun newEntry(name: String) = FileVDMEntry(root.resolve(name), name, writer = this)

    override fun putEntry(entry: VDMEntry): OutputStream =
            if (entry !is FileVDMEntry || entry.writer !== this) {
                throw IllegalArgumentException("Invalid entry $entry")
            } else if (entry.stream != null) {
                throw IllegalArgumentException("Entry is already opened")
            } else {
                val path = entry.path
                if (Files.notExists(path.parent)) {
                    Files.createDirectories(path.parent)
                }
                with(Files.newOutputStream(path)) {
                    entry.stream = this
                    streams += this
                    this
                }
            }

    override fun closeEntry(entry: VDMEntry) {
        if (entry !is FileVDMEntry || entry.writer !== this) {
            throw IllegalArgumentException("Invalid entry $entry")
        } else if (entry.stream == null) {
            throw IllegalArgumentException("Entry is not opened")
        }
        entry.stream?.let {
            streams -= it
            it.flush()
            it.close()
        }
    }

    override fun toString() = "FileVDMWriter@${hashCode()}{root=$root}"

    override fun close() {
        for (stream in streams) {
            stream.flush()
            stream.close()
        }
        streams.clear()
    }

    fun finalize() {
        close()
    }
}

class FileVDMFactory : VDMFactory {
    override val keys = setOf(VDM_DIRECTORY)

    override val name = "Factory for FileVDM"

    override fun getReader(input: Any, props: VariantMap): VDMReader =
            FileVDMReader(getDirectory(input, true))

    override fun getWriter(output: Any, props: VariantMap): VDMWriter =
            FileVDMWriter(getDirectory(output, false))

    private fun getDirectory(arg: Any, reading: Boolean): Path {
        val dir = when (arg) {
            is Path -> arg
            is String -> Paths.get(arg)
            is File -> arg.toPath()
            else -> throw IllegalArgumentException(arg.toString())
        }
        if (reading) {
            if (Files.notExists(dir)) throw FileNotFoundException(dir.toString())
            if (!Files.isDirectory(dir)) throw NotDirectoryException(dir.toString())
        }
        return dir
    }

    override fun toString() = name
}

private class ZipVDMEntry(val entry: ZipEntry, val reader: ZipVDMReader? = null, val writer: ZipVDMWriter? = null) : VDMEntry {
    override val name: String = entry.name

    override val comment: String? = entry.comment

    override val isDirectory = entry.isDirectory

    override val lastModified = entry.time

    override fun toString() =
            reader?.let { "zip:file:/${it.name.slashify()}!$entry" } ?: entry.toString()
}

private class ZipVDMReader(val zip: ZipFile) : DisposableSupport(), VDMReader {
    override val name: String = zip.name

    override val comment: String? = zip.comment

    override fun getEntry(name: String) = zip.getEntry(name)?.let { ZipVDMEntry(it, this) }

    override fun getInputStream(entry: VDMEntry): InputStream =
            if (entry !is ZipVDMEntry || entry.reader !== this) {
                throw IllegalArgumentException("Invalid entry $entry")
            } else {
                zip.getInputStream(entry.entry)
            }

    override val entries
        get() = zip.entries().asSequence().map { ZipVDMEntry(it, this) }.iterator()

    override fun toString() = "ZipVDMReader@${hashCode()}{zip=$name}"

    override val size get() = zip.size()

    override fun close() = zip.close()

    override fun dispose() = close()
}

private class ZipVDMWriter(val zip: ZipOutputStream) : VDMWriter {
    override fun setComment(comment: String) {
        zip.setComment(comment)
    }

    override fun newEntry(name: String) = ZipVDMEntry(ZipEntry(name), writer = this)

    override fun putEntry(entry: VDMEntry): ZipOutputStream =
            if (entry !is ZipVDMEntry || entry.writer !== this) {
                throw IllegalArgumentException("Invalid entry $entry")
            } else {
                zip.apply { zip.putNextEntry(entry.entry) }
            }

    override fun closeEntry(entry: VDMEntry) {
        if (entry !is ZipVDMEntry || entry.writer !== this) {
            throw IllegalArgumentException("Invalid entry $entry")
        }
        zip.closeEntry()
    }

    override fun close() {
        zip.flush()
        zip.close()
    }
}

class ZipVDMFactory : VDMFactory {
    override val keys = setOf(VDM_ZIP)

    override val name = "Factory for ZipVDM"

    override fun getReader(input: Any, props: VariantMap): VDMReader {
        val mode = props["mode"] as Int? ?: ZipFile.OPEN_READ
        val charset = props["charset"] as Charset? ?: Charsets.UTF_8
        return ZipVDMReader(when (input) {
            is String -> ZipFile(File(input), mode, charset)
            is File -> ZipFile(input, mode, charset)
            is ZipFile -> input
            is Path -> ZipFile(input.toFile(), mode, charset)
            else -> throw IllegalArgumentException(input.toString())
        })
    }

    override fun getWriter(output: Any, props: VariantMap): VDMWriter {
        val charset = props["charset"] as Charset? ?: Charsets.UTF_8
        val method = props["method"] as Int? ?: ZipOutputStream.DEFLATED
        val level = props["level"] as Int? ?: Deflater.DEFAULT_COMPRESSION
        val zip = when (output) {
            is ZipOutputStream -> output
            is OutputStream -> ZipOutputStream(output, charset)
            is String -> ZipOutputStream(FileOutputStream(output), charset)
            is File -> ZipOutputStream(output.outputStream(), charset)
            is Path -> ZipOutputStream(output.toFile().outputStream(), charset)
            else -> throw IllegalArgumentException(output.toString())
        }
        zip.setMethod(method)
        zip.setLevel(level)
        return ZipVDMWriter(zip)
    }

    override fun toString() = name
}

fun detectReader(path: Path): VDMReader = if (!Files.isDirectory(path)) {
    ZipVDMReader(ZipFile(path.toFile()))
} else {
    FileVDMReader(path)
}
