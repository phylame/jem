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
import java.io.*
import java.nio.file.Files
import java.nio.file.NotDirectoryException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
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

    override val lastModified = Files.getLastModifiedTime(path).toMillis()

    override val name: String = name + if (isDirectory) "/" else ""

    override fun toString(): String = path.toUri().toString()

    var stream: OutputStream? = null
}

private class FileVDMReader(val root: Path) : DisposableSupport(), VDMReader {
    override val comment = null

    override val name: String = root.toString()

    private val streams = LinkedList<InputStream>()

    override fun getEntry(name: String): FileVDMEntry? {
        require(name.isNotEmpty()) { "name for entry cannot be empty" }
        return root.resolve(name).takeIf { Files.isRegularFile(it) }?.let {
            FileVDMEntry(it, name.replace('\\', '/'), this)
        }
    }

    override fun getInputStream(entry: VDMEntry): InputStream = if (entry !is FileVDMEntry || entry.reader != this) {
        throw IllegalArgumentException("Invalid entry: $entry")
    } else Files.newInputStream(entry.path).also { streams += it }

    override val entries: Iterator<VDMEntry>
        get() {
            val begin = root.toString().length + 1
            return walkRoot().map {
                FileVDMEntry(it, it.toString().substring(begin).replace('\\', '/'), this)
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

    override fun setProperty(name: String, value: Any) {}

    override fun newEntry(name: String) = FileVDMEntry(root.resolve(name), name, writer = this)

    override fun putEntry(entry: VDMEntry): OutputStream = if (entry !is FileVDMEntry || entry.writer !== this) {
        throw IllegalArgumentException("Invalid entry $entry")
    } else if (entry.stream != null) {
        throw IllegalArgumentException("Entry is opened")
    } else {
        if (Files.notExists(root)) {
            Files.createDirectories(root)
        }
        Files.newOutputStream(entry.path).also {
            entry.stream = it
            streams += it
        }
    }

    override fun closeEntry(entry: VDMEntry) {
        if (entry !is FileVDMEntry || entry.writer !== this) {
            throw IllegalArgumentException("Invalid entry $entry")
        } else if (entry.stream == null) {
            throw IllegalArgumentException("Entry is not opened")
        }
        entry.stream?.let {
            it.flush()
            it.close()
            streams -= it
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

    override fun getReader(input: Any): VDMReader = FileVDMReader(getDirectory(input, true))

    override fun getWriter(output: Any): VDMWriter = FileVDMWriter(getDirectory(output, false))

    private fun getDirectory(arg: Any, reading: Boolean): Path {
        val dir = when (arg) {
            is File -> arg.toPath()
            is String -> Paths.get(arg)
            is Path -> arg
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

    override fun toString() = reader?.let {
        "zip:file:/${it.name.replace('\\', '/')}!$entry"
    } ?: entry.toString()
}

private class ZipVDMReader(val zip: ZipFile) : DisposableSupport(), VDMReader {
    override val name: String = zip.name

    override val comment: String? = zip.comment

    override fun getEntry(name: String) = zip.getEntry(name)?.let { ZipVDMEntry(it, this) }

    override fun getInputStream(entry: VDMEntry): InputStream = if (entry !is ZipVDMEntry || entry.reader !== this) {
        throw IllegalArgumentException("Invalid entry $entry")
    } else zip.getInputStream(entry.entry)

    override val entries get() = zip.entries().asSequence().map { ZipVDMEntry(it, this) }.iterator()

    override val size get() = zip.size()

    override fun close() = zip.close()

    override fun dispose() = close()

    override fun toString() = "ZipVDMReader@${hashCode()}{zip=$name}"
}

private class ZipVDMWriter(val zip: ZipOutputStream) : VDMWriter {
    override fun setComment(comment: String) {
        zip.setComment(comment)
    }

    override fun setProperty(name: String, value: Any) {
        when (name) {
            "method" -> zip.setMethod(value as Int)
            "level" -> zip.setLevel(value as Int)
        }
    }

    override fun newEntry(name: String) = ZipVDMEntry(ZipEntry(name), writer = this)

    override fun putEntry(entry: VDMEntry) = if (entry !is ZipVDMEntry || entry.writer !== this) {
        throw IllegalArgumentException("Invalid entry $entry")
    } else zip.apply { zip.putNextEntry(entry.entry) }

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

    override fun getReader(input: Any): VDMReader = ZipVDMReader(when (input) {
        is String -> ZipFile(input)
        is File -> ZipFile(input)
        is ZipFile -> input
        is Path -> ZipFile(input.toFile())
        else -> throw IllegalArgumentException(input.toString())
    })

    override fun getWriter(output: Any): VDMWriter = ZipVDMWriter(when (output) {
        is ZipOutputStream -> output
        is OutputStream -> ZipOutputStream(output)
        is String -> ZipOutputStream(FileOutputStream(output))
        is File -> ZipOutputStream(output.outputStream())
        is Path -> ZipOutputStream(output.toFile().outputStream())
        else -> throw IllegalArgumentException(output.toString())
    })

    override fun toString() = name
}

fun detectReader(path: Path): VDMReader = if (Files.isDirectory(path))
    FileVDMReader(path)
else ZipVDMReader(ZipFile(path.toFile()))
