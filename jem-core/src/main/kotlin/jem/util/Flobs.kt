package jem.util

import jclp.io.copyLimited
import jclp.io.detectMime
import jclp.io.getResource
import jclp.io.clippedStream
import java.io.*
import java.net.URL

interface Flob {
    val name: String

    val mime: String

    fun openStream(): InputStream

    fun writeTo(output: OutputStream) = openStream().copyLimited(output)

    companion object {
        fun of(url: URL, mime: String = ""): Flob = URLFlob(url, mime)

        fun of(file: File, mime: String = ""): Flob = FileFlob(file, mime)

        fun of(data: ByteArray, name: String, mime: String = ""): Flob = ByteFlob(name, data, mime)

        fun of(file: RandomAccessFile, name: String, offset: Long, length: Long, mime: String = ""): Flob {
            return BlockFlob(name, file, offset, length, mime)
        }

        fun of(uri: String, loader: ClassLoader? = null, mime: String = ""): Flob? {
            val url = getResource(uri, loader) ?: return null
            return URLFlob(url, mime)
        }

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

abstract class AbstractFlob(mime: String) : Flob {
    override val mime by lazy { detectMime(name, mime) }

    override fun toString() = "$name;mime=$mime"
}

private class URLFlob(private val url: URL, mime: String) : AbstractFlob(mime) {
    override val name: String = url.path

    override fun openStream(): InputStream = url.openStream()

    override fun toString() = "$url;mime=$mime"
}

private class FileFlob(private val file: File, mime: String) : AbstractFlob(mime) {
    override val name: String = file.path

    override fun openStream() = file.inputStream()
}

private class ByteFlob(override val name: String, private val data: ByteArray, mime: String) : AbstractFlob(mime) {
    override fun openStream() = ByteArrayInputStream(data)

    override fun writeTo(output: OutputStream): Long {
        output.write(data)
        return data.size.toLong()
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

    override fun openStream(): InputStream {
        file.seek(offset)
        return file.clippedStream(offset, length)
    }

    override fun writeTo(output: OutputStream): Long {
        file.seek(offset)
        return file.copyLimited(output, length)
    }
}
