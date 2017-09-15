package jclp.io

import jclp.ServiceManager
import jclp.ServiceProvider
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.io.OutputStream


interface VdmEntry {
    val name: String

    val comment: String?

    val lastModified: Long

    val isDirectory: Boolean
}

interface VdmReader : Closeable {
    val name: String

    val comment: String?

    fun getEntry(name: String): VdmEntry?

    fun getInputStream(entry: VdmEntry): InputStream

    val entries: Iterator<VdmEntry>

    val size: Int
}

interface VdmWriter : Closeable {
    fun setComment(comment: String)

    fun setProperty(name: String, value: Any)

    fun newEntry(name: String): VdmEntry

    fun putEntry(entry: VdmEntry): OutputStream

    fun closeEntry(entry: VdmEntry)

    fun write(entry: VdmEntry, b: ByteArray)

    fun write(entry: VdmEntry, b: ByteArray, off: Int, len: Int)
}

interface VdmFactory : ServiceProvider {
    fun getReader(input: Any): VdmReader

    fun getWriter(output: Any): VdmWriter
}

object VdmManager : ServiceManager<VdmFactory>(VdmFactory::class.java) {
    fun getReader(name: String, input: Any) = getService(name)?.getReader(input)

    fun getWriter(name: String, output: Any) = getService(name)?.getWriter(output)
}

private class FileVDMReader(val dir: File) : VdmReader {
    override val comment = null

    override val name: String = dir.path

    override fun getEntry(name: String): VdmEntry {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getInputStream(entry: VdmEntry): InputStream {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val entries: Iterator<VdmEntry>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val size: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

private class FileVDMWriter:VdmWriter{
    override fun setComment(comment: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setProperty(name: String, value: Any) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun newEntry(name: String): VdmEntry {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun putEntry(entry: VdmEntry): OutputStream {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun closeEntry(entry: VdmEntry) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun write(entry: VdmEntry, b: ByteArray) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun write(entry: VdmEntry, b: ByteArray, off: Int, len: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}