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

import jclp.io.createRecursively
import jclp.synchronized
import java.io.*
import java.nio.file.NotDirectoryException
import java.nio.file.Path
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

private class FileVDMEntry(
        val file: File, name: String, val reader: FileVDMReader? = null, val writer: FileVDMWriter? = null
) : VDMEntry {
    override val comment = null

    override val isDirectory = file.isDirectory

    override val lastModified = file.lastModified()

    override val name = name + if (file.isDirectory) "/" else ""

    override fun toString() = "file:/${file.canonicalPath.replace('\\', '/')}"

    var stream: OutputStream? = null
}

private class FileVDMReader(val dir: File) : VDMReader {
    override val comment = null

    override val name: String = dir.path

    private val streams = LinkedList<InputStream>().synchronized()

    override fun getEntry(name: String) = File(dir, name).takeIf(File::exists)?.let {
        FileVDMEntry(it, name.replace('\\', '/'), this)
    }

    override fun getInputStream(entry: VDMEntry) = if (entry !is FileVDMEntry || entry.reader != this) {
        throw IllegalArgumentException("Invalid entry: $entry")
    } else {
        FileInputStream(entry.file).apply { streams += this }
    }

    override val entries: Iterator<VDMEntry>
        get() {
            val begin = dir.path.length + 1
            return walkDir().map {
                FileVDMEntry(it, it.path.substring(begin).replace('\\', '/'), this)
            }.iterator()
        }

    override val size get() = walkDir().count()

    private fun walkDir() = dir.walkTopDown().filter { it !== dir }

    override fun toString() = "FileVDMReader@${hashCode()}{dir=$dir}"

    override fun close() {
        streams.onEach(InputStream::close).clear()
    }

    fun finalize() {
        close()
    }
}

private class FileVDMWriter(val dir: File) : VDMWriter {
    private val streams = LinkedList<OutputStream>().synchronized()

    override fun setComment(comment: String) {}

    override fun setProperty(name: String, value: Any) {}

    override fun newEntry(name: String) = FileVDMEntry(File(dir, name), name, writer = this)

    override fun putEntry(entry: VDMEntry) = if (entry !is FileVDMEntry || entry.writer !== this) {
        throw IllegalArgumentException("Invalid entry $entry")
    } else if (!entry.file.parentFile.createRecursively()) {
        throw IOException("Cannot create directory ${entry.file.parent}")
    } else if (entry.stream != null) {
        throw IllegalArgumentException("Entry is opened")
    } else {
        FileOutputStream(entry.file).apply {
            entry.stream = this
            streams += this
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

    override fun toString() = "FileVDMWriter@${hashCode()}{dir=$dir}"

    override fun close() {
        streams.onEach {
            it.flush()
            it.close()
        }.clear()
    }

    fun finalize() {
        close()
    }
}

class FileVDMFactory : VDMFactory {
    override val keys = setOf("dir")

    override val name = "Factory for FileVDM"

    override fun getReader(input: Any): VDMReader = FileVDMReader(getDirectory(input, true))

    override fun getWriter(output: Any): VDMWriter = FileVDMWriter(getDirectory(output, false))

    private fun getDirectory(arg: Any, reading: Boolean): File {
        val dir = when (arg) {
            is File -> arg
            is String -> File(arg)
            is Path -> arg.toFile()
            else -> throw IllegalArgumentException(arg.toString())
        }
        if (reading) {
            if (!dir.exists()) throw FileNotFoundException(dir.path)
            if (!dir.isDirectory) throw NotDirectoryException(dir.path)
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

private class ZipVDMReader(val zip: ZipFile) : VDMReader {
    override val name: String = zip.name

    override val comment: String? = zip.comment

    override fun getEntry(name: String) = zip.getEntry(name)?.let { ZipVDMEntry(it, this) }

    override fun getInputStream(entry: VDMEntry): InputStream = if (entry !is ZipVDMEntry || entry.reader !== this) {
        throw IllegalArgumentException("Invalid entry $entry")
    } else zip.getInputStream(entry.entry)

    override val entries get() = zip.entries().asSequence().map { ZipVDMEntry(it, this) }.iterator()

    override val size get() = zip.size()

    override fun close() {
        zip.close()
    }

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
    override val keys = setOf("zip")

    override val name = "Factory for ZipVDM"

    override fun getReader(input: Any): VDMReader = ZipVDMReader(when (input) {
        is String -> ZipFile(input)
        is File -> ZipFile(input)
        is ZipFile -> input
        is Path -> ZipFile(input.toFile())
        else -> throw IllegalArgumentException(input.toString())
    })

    override fun getWriter(output: Any): VDMWriter = ZipVDMWriter(when (output) {
        is String -> ZipOutputStream(FileOutputStream(output))
        is File -> ZipOutputStream(output.outputStream())
        is ZipOutputStream -> output
        is OutputStream -> ZipOutputStream(output)
        is Path -> ZipOutputStream(output.toFile().outputStream())
        else -> throw IllegalArgumentException(output.toString())
    })

    override fun toString() = name
}

fun detectReader(file: File) = if (file.isDirectory) FileVDMReader(file) else ZipVDMReader(ZipFile(file))
