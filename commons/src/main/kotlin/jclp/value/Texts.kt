package jclp.value

import java.io.Writer
import java.nio.charset.Charset

const val TEXT_HTML = "html"
const val TEXT_PLAIN = "plain"

interface Text : Iterable<String> {
    val type get() = TEXT_PLAIN

    override fun toString(): String

    override fun iterator() = toString().split("\r\n", "\n", "\r").iterator()

    fun writeTo(output: Writer) = toString().let {
        output.write(it)
        it.length.toLong()
    }

    companion object {
        fun empty(type: String = TEXT_PLAIN) = of("", type)

        fun of(cs: CharSequence, type: String = TEXT_PLAIN): Text = RawText(type, cs)

        fun of(flob: Flob, encoding: String? = null, type: String = TEXT_PLAIN): Text = FlobText(type, flob, encoding)
    }
}

open class TextWrapper(private val text: Text) : Text {
    override val type = text.type

    override fun toString() = text.toString()

    override fun iterator() = text.iterator()

    override fun writeTo(output: Writer) = text.writeTo(output)

    override fun equals(other: Any?) = text == other

    override fun hashCode() = text.hashCode()
}

abstract class AbstractText(final override val type: String) : Text {
    init {
        require(type.isNotEmpty()) { "type cannot be empty" }
    }
}

private class RawText(type: String, private val text: CharSequence) : AbstractText(type) {
    override fun toString() = text.toString()
}

private class FlobText(type: String, private val flob: Flob, encoding: String? = null) : AbstractText(type) {
    private val charset: Charset = when {
        encoding == null -> Charset.defaultCharset()
        encoding.isNotEmpty() -> Charset.forName(encoding)
        else -> throw IllegalArgumentException("Unsupported encoding: $encoding")
    }

    override fun toString() = openReader().readText()

    override fun iterator() = openReader().readLines().iterator()

    override fun writeTo(output: Writer) = openReader().copyTo(output)

    private fun openReader() = flob.openStream().bufferedReader(charset)
}
