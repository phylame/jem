package jclp.text

import jclp.DisposableSupport
import jclp.io.Flob
import jclp.io.LineIterator
import jclp.io.writeLines
import jclp.release
import jclp.retain
import java.io.Writer
import java.nio.charset.Charset

const val TEXT_HTML = "html"
const val TEXT_PLAIN = "plain"

interface Text {
    val type get() = TEXT_PLAIN

    override fun toString(): String

    operator fun iterator(): Iterator<String> = LineSplitter(toString())

    fun writeTo(output: Writer) = output.write(toString())
}

open class TextWrapper(val text: Text) : Text {
    override val type = text.type

    override fun toString() = text.toString()

    override fun iterator() = text.iterator()

    override fun writeTo(output: Writer) = text.writeTo(output)

    override fun equals(other: Any?) = text == other

    override fun hashCode() = text.hashCode()
}

private class StringText(val text: CharSequence, override val type: String) : Text {
    override fun toString() = text.toString()
}

fun textOf(text: CharSequence, type: String = TEXT_PLAIN): Text {
    require(type.isNotEmpty()) { "type cannot be empty" }
    return StringText(text, type)
}

fun emptyText(type: String = TEXT_PLAIN) = textOf("", type)

abstract class IteratorText(final override val type: String) : Text {
    init {
        require(type.isNotEmpty()) { "type cannot be empty" }
    }

    abstract override fun iterator(): Iterator<String>

    override fun toString() = iterator().asSequence().joinToString(System.lineSeparator())

    override fun writeTo(output: Writer) {
        output.writeLines(iterator(), System.lineSeparator())
    }
}

fun textOf(iterator: Iterator<String>, type: String = TEXT_PLAIN) = object : IteratorText(type) {
    override fun iterator() = iterator
}

private class FlobText(val flob: Flob, val charset: Charset, override val type: String) : DisposableSupport(), Text {
    init {
        flob.retain()
    }

    override fun toString() = openReader().use { it.readText() }

    override fun iterator() = LineIterator(openReader(), true)

    private fun openReader() = flob.openStream().bufferedReader(charset)

    override fun writeTo(output: Writer) {
        openReader().use { it.copyTo(output) }
    }

    override fun dispose() {
        flob.release()
    }
}

fun textOf(flob: Flob, charset: Charset? = null, type: String = TEXT_PLAIN): Text {
    require(type.isNotEmpty()) { "type cannot be empty" }
    return FlobText(flob, charset ?: Charset.defaultCharset(), type)
}
