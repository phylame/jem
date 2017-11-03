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

package jclp.io

import java.io.*
import java.util.*

fun InputStream.copyRange(output: OutputStream, size: Long = -1, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    return reading().copyRange(output.writing(), size, bufferSize)
}

fun InputStream.copyRange(output: RandomAccessFile, size: Long = -1, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    return reading().copyRange(output.writing(), size, bufferSize)
}

fun RandomAccessFile.copyRange(output: OutputStream, size: Long = -1, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    return reading().copyRange(output.writing(), size, bufferSize)
}

fun RandomAccessFile.copyRange(output: RandomAccessFile, size: Long = -1, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    return reading().copyRange(output.writing(), size, bufferSize)
}

fun RandomAccessFile.clippedStream(offset: Long = -1, length: Long = -1): InputStream {
    return ClippedInputStream(this, if (offset < 0) filePointer else offset, if (length < 0) this.length() else length)
}

fun RandomAccessFile.readBytes(estimatedSize: Int = DEFAULT_BUFFER_SIZE): ByteArray {
    val buffer = ByteArrayOutputStream(maxOf((length() - filePointer).toInt(), estimatedSize))
    copyRange(buffer)
    return buffer.toByteArray()
}

interface ByteInput {
    fun read(b: ByteArray, off: Int, len: Int): Int
}

fun InputStream.reading() = object : ByteInput {
    override fun read(b: ByteArray, off: Int, len: Int) = this@reading.read(b, off, len)
}

fun RandomAccessFile.reading() = object : ByteInput {
    override fun read(b: ByteArray, off: Int, len: Int) = this@reading.read(b, off, len)
}

interface ByteOutput {
    fun write(b: ByteArray, off: Int, len: Int)
}

fun OutputStream.writing() = object : ByteOutput {
    override fun write(b: ByteArray, off: Int, len: Int) {
        this@writing.write(b, off, len)
    }
}

fun RandomAccessFile.writing() = object : ByteOutput {
    override fun write(b: ByteArray, off: Int, len: Int) {
        this@writing.write(b, off, len)
    }
}

fun ByteInput.copyRange(output: ByteOutput, size: Long = -1, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    require(bufferSize > 0) { "bufferSize($bufferSize) <= 0" }
    if (size == 0L) return 0
    var total = 0L
    val buffer = ByteArray(bufferSize)
    var bytes: Int = read(buffer, 0, bufferSize)
    while (bytes != -1) {
        total += bytes.toLong()
        if (size < 0 || total < size) {
            output.write(buffer, 0, bytes)
        } else {
            output.write(buffer, 0, bytes - (total - size).toInt())
            total = size
            break
        }
        bytes = read(buffer, 0, bufferSize)
    }
    return total
}

fun Reader.copyRange(output: Writer, size: Long = -1, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    require(bufferSize > 0) { "bufferSize($bufferSize) <= 0" }
    if (size == 0L) return 0
    var total = 0L
    val buffer = CharArray(bufferSize)
    var chars: Int = read(buffer, 0, bufferSize)
    while (chars != -1) {
        total += chars.toLong()
        if (size < 0 || total < size) {
            output.write(buffer, 0, chars)
        } else {
            output.write(buffer, 0, chars - (total - size).toInt())
            total = size
            break
        }
        chars = read(buffer, 0, bufferSize)
    }
    return total
}

fun Writer.writeLines(lines: Iterator<*>, separator: String = System.lineSeparator()) {
    while (lines.hasNext()) {
        write(lines.next().toString())
        if (lines.hasNext()) {
            write(separator)
        }
    }
}

class LineIterator(private val reader: BufferedReader, private val closeOnEnd: Boolean = false) : Iterator<String> {
    private var isDone = false
    private var nextLine: String? = null

    override fun hasNext(): Boolean {
        if (nextLine == null && !isDone) {
            nextLine = reader.readLine()
            if (nextLine == null) {
                isDone = true
                if (closeOnEnd) {
                    reader.close()
                }
            }
        }
        return nextLine != null
    }

    override fun next(): String {
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        val answer = nextLine
        nextLine = null
        return answer!!
    }
}

private class ClippedInputStream(private val source: RandomAccessFile, offset: Long, size: Long) : InputStream() {
    private val endpos: Long
    private var curpos: Long = 0

    init {
        val length = source.length()
        curpos = if (offset < 0) 0 else offset
        endpos = if (size < 0) length else curpos + size
        require(curpos < length) { "offset >= length of source" }
        require(endpos <= length) { "offset + size > length of source" }
    }

    override fun read() = if (curpos < endpos) {
        ++curpos
        source.read()
    } else {
        -1
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (off < 0 || len < 0 || len > b.size - off) {
            throw IndexOutOfBoundsException()
        } else if (len == 0) {
            return 0
        }
        var count = endpos - curpos
        if (count == 0L) {
            return -1
        }
        count = if (count < len) count else len.toLong()
        val bytes = source.read(b, off, count.toInt())
        curpos += count
        return bytes
    }

    override fun skip(n: Long): Long {
        if (n < 0) return 0
        val bytes = source.skipBytes(minOf(n, endpos - curpos).toInt()).toLong()
        curpos = minOf(curpos + bytes, endpos)
        return bytes
    }

    override fun available() = (endpos - curpos).toInt()
}
