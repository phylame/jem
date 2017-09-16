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

package jclp.io

import java.io.*

fun InputStream.copyLimited(output: OutputStream, size: Long = -1, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    return reading().copyLimited(output.writing(), size, bufferSize)
}

fun InputStream.copyLimited(output: RandomAccessFile, size: Long = -1, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    return reading().copyLimited(output.writing(), size, bufferSize)
}

fun RandomAccessFile.copyLimited(output: OutputStream, size: Long = -1, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    return reading().copyLimited(output.writing(), size, bufferSize)
}

fun RandomAccessFile.copyLimited(output: RandomAccessFile, size: Long = -1, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    return reading().copyLimited(output.writing(), size, bufferSize)
}

fun RandomAccessFile.clippedStream(offset: Long = -1, length: Long = -1): InputStream {
    return ClippedInputStream(this, if (offset < 0) filePointer else offset, if (length < 0) this.length() else length)
}

fun RandomAccessFile.readBytes(estimatedSize: Int = DEFAULT_BUFFER_SIZE): ByteArray {
    val buffer = ByteArrayOutputStream(maxOf((length() - filePointer).toInt(), estimatedSize))
    copyLimited(buffer)
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

fun ByteInput.copyLimited(output: ByteOutput, size: Long = -1, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    require(bufferSize > 0) { "bufferSize <= 0" }
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

fun Reader.copyLimited(output: Writer, size: Long = -1, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    require(bufferSize > 0) { "bufferSize <= 0" }
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

fun Writer.writeLines(lines: Collection<CharSequence>, lineSeparator: String = System.lineSeparator()) {
    val end = lines.size
    lines.forEachIndexed { i, line ->
        write(line.toString())
        if (i + 1 != end) {
            write(lineSeparator)
        }
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

    @Throws(IOException::class)
    override fun read() = if (curpos < endpos) {
        ++curpos
        source.read()
    } else {
        -1
    }

    @Throws(IOException::class)
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

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        if (n < 0) {
            return 0
        }
        val bytes = source.skipBytes(Math.min(n, endpos - curpos).toInt()).toLong()
        curpos = Math.min(curpos + bytes, endpos)
        return bytes
    }

    @Throws(IOException::class)
    override fun available() = (endpos - curpos).toInt()
}
