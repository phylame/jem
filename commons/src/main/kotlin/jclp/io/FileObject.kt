package jclp.io

import jclp.DisposableSupport
import jclp.release
import jclp.retain
import jclp.text.or
import jclp.vdm.VDMReader
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

fun flobOf(uri: String, loader: ClassLoader? = null, mime: String = ""): Flob {
    return getResource(uri, loader)?.let { flobOf(it, mime) } ?: throw IOException("No such resource: $uri")
}

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

private class VDMFlob(val reader: VDMReader, override val name: String, mime: String) : DisposableSupport(), Flob {
    val entry = reader.getEntry(name) ?: throw IOException("No such entry: $name")

    init {
        reader.retain()
    }

    override val mimeType = mime or { mimeType(name) }

    override fun openStream() = reader.getInputStream(entry)

    override fun toString() = "$entry;mime=$mimeType"

    override fun dispose() {
        reader.release()
    }
}

fun flobOf(reader: VDMReader, name: String, mime: String = ""): Flob = VDMFlob(reader, name, mime)
