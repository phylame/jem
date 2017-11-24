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

package jem.format.util

import jclp.Linguist
import jclp.looseISODate
import jclp.looseISODateTime
import jclp.looseISOTime
import jclp.setting.Settings
import jclp.setting.getString
import jem.epm.MakerException
import jem.epm.ParserException
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.OutputStream
import java.io.Writer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

internal object M : Linguist("!jem/format/util/messages")

fun failParser(key: String, vararg args: Any): Nothing = throw ParserException(M.tr(key, *args))

fun failParser(e: Throwable, key: String, vararg args: Any): Nothing = throw ParserException(M.tr(key, *args), e)

inline fun parseInt(text: String, error: () -> String) = try {
    text.toInt()
} catch (e: NumberFormatException) {
    failParser(e, "err.parser.badNumber", text, error())
}

inline fun parseDouble(text: String, error: () -> String) = try {
    text.toDouble()
} catch (e: NumberFormatException) {
    failParser(e, "err.parser.badNumber", text, error())
}

inline fun parseDateTime(text: String, format: String, error: () -> String): LocalDateTime = try {
    LocalDateTime.parse(text, if (format.isNotEmpty()) DateTimeFormatter.ofPattern(format) else looseISODateTime)
} catch (e: Exception) {
    failParser(e, "err.parser.badDate", text, error())
}

inline fun parseDate(text: String, format: String, error: () -> String): LocalDate = try {
    LocalDate.parse(text, if (format.isNotEmpty()) DateTimeFormatter.ofPattern(format) else looseISODate)
} catch (e: Exception) {
    failParser(e, "err.parser.badDate", text, error())
}

inline fun parseTime(text: String, format: String, error: () -> String): LocalTime = try {
    LocalTime.parse(text, if (format.isNotEmpty()) DateTimeFormatter.ofPattern(format) else looseISOTime)
} catch (e: Exception) {
    failParser(e, "err.parser.badDate", text, error())
}

fun xmlAttribute(xpp: XmlPullParser, name: String, where: Any, namespace: String? = null): String =
        xpp.getAttributeValue(namespace, name) ?: failParser("err.parser.noAttribute", name, xpp.name, where, xpp.lineNumber)

inline fun withXml(xpp: XmlPullParser, where: Any, action: (Boolean, StringBuilder) -> Boolean) {
    var hasText = false
    val b = StringBuilder()
    try {
        var event = xpp.eventType
        do {
            when (event) {
                XmlPullParser.START_TAG -> hasText = action(true, b)
                XmlPullParser.END_TAG -> b.let { action(false, it); it.setLength(0) }
                XmlPullParser.TEXT -> if (hasText) b.append(xpp.text)
            }
            event = xpp.next()
        } while (event != XmlPullParser.END_DOCUMENT)
    } catch (e: XmlPullParserException) {
        failParser(e, "err.parser.badXml", where)
    }
}

fun failMaker(key: String, vararg args: Any): Nothing = throw MakerException(M.tr(key, *args))

class XmlRender(settings: Settings?) {
    private var depth: Int = 0

    private val tags = LinkedList<Tag>()

    private val indent = settings?.getString("maker.xml.indent") ?: "\t"

    private val encoding = settings?.getString("maker.xml.encoding") ?: "UTF-8"

    private val separator = settings?.getString("maker.xml.separator") ?: "\n"

    private val serializer = XmlPullParserFactory.newInstance().newSerializer()

    fun output(stream: OutputStream, encoding: String? = null): XmlRender {
        serializer.setOutput(stream, encoding ?: this.encoding)
        return this
    }

    fun output(writer: Writer): XmlRender {
        serializer.setOutput(writer)
        return this
    }

    fun flush(): XmlRender {
        serializer.flush()
        return this
    }

    fun reset(): XmlRender {
        depth = 0
        tags.clear()
        return this
    }

    fun beginXml(standalone: Boolean = true): XmlRender {
        serializer.startDocument(encoding, standalone)
        return reset()
    }

    fun docdecl(root: String, id: String, url: String): XmlRender {
        return docdecl("$root PUBLIC \"$id\" \"$url\"")
    }

    fun docdecl(text: String): XmlRender {
        newLine()
        serializer.docdecl(" " + text)
        return this
    }

    fun beginTag(name: String, namespace: String? = null): XmlRender {
        newNode()
        ++depth
        serializer.startTag(namespace, name)
        tags.push(Tag(name, namespace))
        return this
    }

    fun attribute(name: String, value: String, namespace: String? = null): XmlRender {
        serializer.attribute(namespace, name, value)
        return this
    }

    fun xmlns(namespace: String) = attribute("xmlns", namespace)

    fun comment(text: String): XmlRender {
        newNode()
        serializer.comment(text)
        return this
    }

    fun text(text: String): XmlRender {
        serializer.text(text)
        return this
    }

    fun endTag(): XmlRender {
        check(tags.isNotEmpty()) { "startTag should be called firstly" }
        val tag = tags.pop()
        if (tag.hasChild) {
            newLine()
            indent(depth - 1)
        }
        --depth
        serializer.endTag(tag.namespace, tag.name)
        return this
    }

    fun endXml() {
        serializer.endDocument()
        flush()
    }

    private fun indent(count: Int) {
        if (count > 0) serializer.text(indent.repeat(count))
    }

    private fun newLine() {
        serializer.text(separator)
    }

    private fun newNode() {
        newLine()
        indent(depth)
        tags.firstOrNull()?.hasChild = true
    }

    private data class Tag(val name: String, val namespace: String?) {
        var hasChild = false
    }
}
