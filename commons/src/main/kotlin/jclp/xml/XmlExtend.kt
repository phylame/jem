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

package jclp.xml

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer
import java.io.Flushable
import java.io.OutputStream
import java.io.Writer

const val SERIALIZER_INDENTATION_KEY = "http://xmlpull.org/v1/doc/properties.html#serializer-indentation"
const val SERIALIZER_LINE_SEPARATOR_KEY = "http://xmlpull.org/v1/doc/properties.html#serializer-line-separator"

var XmlSerializer.indentation: String?
    inline get() = getProperty(SERIALIZER_INDENTATION_KEY) as? String
    inline set(value) = setProperty(SERIALIZER_INDENTATION_KEY, value)

var XmlSerializer.lineSeparator: String?
    inline get() = getProperty(SERIALIZER_LINE_SEPARATOR_KEY) as? String
    inline set(value) = setProperty(SERIALIZER_LINE_SEPARATOR_KEY, value)

fun XmlSerializer.init(output: Writer, indent: String = "  ", newLine: String = System.lineSeparator()) {
    setOutput(output)
    lineSeparator = newLine
    indentation = indent
}

fun XmlSerializer.init(output: OutputStream, encoding: String = "UTF-8", indent: String = "  ", newLine: String = System.lineSeparator()) {
    setOutput(output, encoding)
    lineSeparator = newLine
    indentation = indent
}

fun XmlSerializer.startDocument(encoding: String = "UTF-8") {
    startDocument(encoding, true)
    text(lineSeparator)
}

fun XmlSerializer.doctype(root: String) {
    docdecl(" $root")
    text(lineSeparator)
}

fun XmlSerializer.docdecl(root: String, id: String, uri: String) {
    docdecl(""" $root PUBLIC "$id" "$uri"""")
    text(lineSeparator)
}

fun XmlSerializer.startTag(name: String) {
    startTag(null, name)
}

fun XmlSerializer.startTag(prefix: String, namespace: String, name: String) {
    setPrefix(prefix, namespace)
    startTag(null, name)
}

fun XmlSerializer.attribute(name: String, value: String) {
    attribute(null, name, value)
}

fun XmlSerializer.lang(lang: String) {
    attribute("xml:lang", lang)
}

fun XmlSerializer.comm(text: String) {
    val ln = lineSeparator
    val indent = indentation ?: ""
    text(ln)
    text(indent.repeat(depth))
    comment(text)
    text(ln)
    text(indent.repeat(depth))
}

fun XmlSerializer.endTag() {
    endTag(namespace, name)
}

fun newSerializer(output: Writer, indent: String = "  ", newLine: String = System.lineSeparator()): XmlSerializer =
        XmlPullParserFactory.newInstance().newSerializer().apply { init(output, indent, newLine) }

fun newSerializer(output: OutputStream, encoding: String = "UTF-8", indent: String = "  ", newLine: String = System.lineSeparator()): XmlSerializer =
        XmlPullParserFactory.newInstance().newSerializer().apply { init(output, encoding, indent, newLine) }

fun XmlPullParser.getAttribute(name: String): String = getAttributeValue(null, name)

class XML(val output: Appendable, val indent: String, val lineSeparator: String) {
    fun doctype(root: String, scope: String = "", dtd: String = "", uri: String = "") {
        with(output) {
            append("<!DOCTYPE $root")
            if (scope.isNotEmpty()) append(" $scope")
            if (dtd.isNotEmpty()) append(" \"$dtd\"")
            if (uri.isNotEmpty()) append(" \"$uri\"")
            append(">").append(lineSeparator)
        }
    }

    inline fun tag(name: String, block: Tag.() -> Unit) {
        Tag(name).apply(block).renderTo(output, indent, lineSeparator)
    }
}

class Tag(val name: String, text: String = "") {
    var depth = 0

    val attr = hashMapOf<String, String>()

    val children = arrayListOf<Tag>()

    val text = StringBuilder(text)

    operator fun CharSequence.unaryPlus() {
        text.append(this)
    }

    fun tag(name: String) {
        children += Tag(name)
    }

    fun tag(name: String, text: String) {
        children += Tag(name, text).also { it.depth = depth + 1 }
    }

    inline fun tag(name: String, block: Tag.() -> Unit) {
        children += Tag(name).also { it.depth = depth + 1 }.apply(block)
    }

    fun renderTo(output: Appendable, indent: String, lineSeparator: String) {
        with(output) {
            val prefix = indent.repeat(depth)
            append(prefix).append("<$name")
            if (attr.isNotEmpty()) {
                for (entry in attr) {
                    append(' ').append("${entry.key}=\"${entry.value}\"")
                }
            }
            var hasContent = false
            if (text.isNotEmpty()) {
                append('>').append(text)
                hasContent = true
            }
            if (children.isNotEmpty()) {
                append('>').append(lineSeparator)
                for (tag in children) {
                    tag.renderTo(this, indent, lineSeparator)
                    append(lineSeparator)
                }
                append(prefix)
                hasContent = true
            }
            if (hasContent) {
                append("</$name>")
            } else {
                append(" />")
            }
        }
    }
}

inline fun Appendable.xml(
        indent: String = "",
        lineSeparator: String = System.lineSeparator(),
        encoding: String = "utf-8",
        standalone: Boolean = true,
        block: XML.() -> Unit) {
    append("<?xml version=\"1.0\" encoding=\"$encoding\"")
    if (!standalone) append(" standalone=\"no\"")
    append("?>").append(lineSeparator)
    XML(this, indent, lineSeparator).apply(block)
    (this as? Flushable)?.flush()
}
