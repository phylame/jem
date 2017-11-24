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

package jem.format.pmab

import jclp.TypeManager
import jclp.ValueMap
import jclp.io.flobOf
import jclp.release
import jclp.setting.Settings
import jclp.setting.getString
import jclp.text.ConverterManager
import jclp.text.Text
import jclp.text.textOf
import jclp.text.valueFor
import jclp.vdm.VdmEntry
import jclp.vdm.VdmReader
import jclp.vdm.readText
import jem.*
import jem.epm.VDMParser
import jem.format.util.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*

internal object PmabParser : VDMParser {
    override fun parse(input: VdmReader, arguments: Settings?) = if (input.readText(PMAB.MIME_PATH) != PMAB.MIME_PMAB) {
        fail("pmab.parse.badMime", PMAB.MIME_PATH, PMAB.MIME_PMAB)
    } else Book().apply {
        val data = Local(this, input, arguments)
        try {
            parsePBM(data)
            parsePBC(data)
        } catch (e: Exception) {
            cleanup()
            throw e
        }
    }

    private fun parsePBM(data: Local) {
        var version = 0
        val xpp = data.newXpp()
        data.openStream(PMAB.PBM_PATH).use {
            xpp.setInput(it, null)
            useXml(xpp, PMAB.PBM_PATH) { begin, sb ->
                if (begin) {
                    var hasText = false
                    when {
                        version == 3 -> hasText = beginPBMv3(xpp.name, data)
                        version != 0 -> TODO("PBM v$version is not implemented")
                        xpp.name == "pbm" -> version = getVersion(xpp, "pmab.parse.unsupportedPBM", data)
                    }
                    hasText
                } else {
                    when (version) {
                        3 -> endPBMv3(xpp.name, sb, data)
                    }
                    false
                }
            }
        }
    }

    private fun parsePBC(data: Local) {
        var version = 0
        val xpp = data.newXpp()
        data.openStream(PMAB.PBC_PATH).use {
            xpp.setInput(it, null)
            useXml(xpp, PMAB.PBC_PATH) { start, sb ->
                if (start) {
                    var hasText = false
                    when {
                        version == 3 -> hasText = beginPBCv3(xpp.name, data)
                        version != 0 -> TODO("PBC v$version is not implemented")
                        xpp.name == "pbc" -> version = getVersion(xpp, "pmab.parse.unsupportedPBC", data)
                    }
                    hasText
                } else {
                    when (version) {
                        3 -> endPBCv3(xpp.name, sb, data)
                    }
                    false
                }
            }
        }
    }

    private fun beginPBMv3(tag: String, data: Local): Boolean {
        val xpp = data.xpp
        var hasText = false
        when (tag) {
            "item" -> {
                data.itemName = data.xmlAttribute("name")
                data.itemType = xpp.getAttributeValue(null, "type")
                hasText = true
            }
            "attributes" -> data.values = data.book.attributes
            "extensions" -> data.values = data.book.extensions
            "meta" -> data.meta[data.xmlAttribute("name")] = data.xmlAttribute("value")
            "head" -> data.meta = hashMapOf()
        }
        return hasText
    }

    private fun endPBMv3(tag: String, sb: StringBuilder, data: Local) {
        if (tag == "item") {
            parseItem(sb.toString().trim(), data).also {
                data.values[data.itemName] = it
                it.release()
            }
        }
    }

    private fun beginPBCv3(tag: String, data: Local): Boolean {
        val xpp = data.xpp
        var hasText = false
        when (tag) {
            "item" -> {
                data.itemName = data.xmlAttribute("name")
                data.itemType = xpp.getAttributeValue(null, "type")
                hasText = true
            }
            "chapter" -> data.newChapter()
            "content" -> {
                data.itemType = xpp.getAttributeValue(null, "type")
                hasText = true
            }
        }
        return hasText
    }

    private fun endPBCv3(tag: String, sb: StringBuilder, data: Local) {
        when (tag) {
            "chapter" -> data.chapter = data.chapter.parent!!
            "item" -> parseItem(sb.toString().trim(), data).also {
                data.chapter[data.itemName] = it
                it.release()
            }
            "content" -> (parseItem(sb.toString().trim(), data) as Text).also {
                data.chapter.text = it
                it.release()
            }
        }
    }

    private fun fallbackType(name: String, type: String): String {
        if (type == TypeManager.STRING) {
            Attributes.getType(name)?.let { return it }
        }
        return when (name) {
            WORDS -> TypeManager.STRING
            else -> type
        }
    }

    private fun parseItem(text: String, data: Local): Any {
        val itemType = data.itemType?.let {
            fallbackType(data.itemName, data.itemType!!)
        } ?: Attributes.getType(data.itemName) ?: return text
        val parts = itemType.split(';')
        val type = parts.first()
        return when (type) {
            TypeManager.STRING, "" -> text
            TypeManager.REAL -> parseDouble(text) { data.xmlPosition() }
            TypeManager.BOOLEAN -> text.toBoolean()
            TypeManager.INTEGER, "unit" -> parseInt(text) { data.xmlPosition() }
            TypeManager.LOCALE -> Locale.forLanguageTag(text.replace('_', '-'))
            TypeManager.DATETIME -> {
                val format = data.getConfig("datetimeFormat") ?: itemType.valueFor("format") ?: ""
                if ("h" in format || "H" in format) {
                    parseDateTime(text, format) { data.xmlPosition() }.let {
                        @Suppress("IMPLICIT_CAST_TO_ANY")
                        if (data.itemName == PUBDATE || data.itemName == DATE) it.toLocalDate() else it
                    }
                } else {
                    parseDate(text, format) { data.xmlPosition() }
                }
            }
            TypeManager.DATE -> {
                val format = data.getConfig("dateFormat") ?: itemType.valueFor("format") ?: ""
                parseDate(text, format) { data.xmlPosition() }
            }
            TypeManager.TIME -> {
                val format = data.getConfig("timeFormat") ?: itemType.valueFor("format") ?: ""
                parseTime(text, format) { data.xmlPosition() }
            }
            else -> when {
                type.startsWith("text/") -> {
                    val encoding = data.getConfig("encoding") ?: itemType.valueFor("encoding")
                    val flob = flobOf(data.reader, text, type)
                    textOf(flob, encoding?.let { Charset.forName(it) }, type.substring(5)).also { flob.release() }
                }
                type.matches("[\\w]+/.+".toRegex()) -> flobOf(data.reader, text, type)
                else -> TypeManager.getClass(type)?.let { ConverterManager.parse(text, it) } ?: text
            }
        }
    }

    private fun getVersion(xpp: XmlPullParser, key: String, data: Local) = data.xmlAttribute("version").let {
        when (it) {
            "3.0" -> 3
            "2.0" -> 2
            else -> fail(key, it, xpp.lineNumber, data.entry)
        }
    }

    private data class Local(val book: Book, val reader: VdmReader, val arguments: Settings?) {
        var chapter: Chapter = book

        var itemType: String? = null

        lateinit var itemName: String

        lateinit var values: ValueMap

        lateinit var meta: MutableMap<String, String>

        lateinit var xpp: XmlPullParser

        lateinit var entry: VdmEntry

        fun newXpp(): XmlPullParser {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            xpp = factory.newPullParser()
            return xpp
        }

        fun xmlAttribute(name: String) = xmlAttribute(xpp, name, entry)

        fun getConfig(key: String) = arguments?.getString("parser.pmab.$key")

        fun xmlPosition() = "${xpp.lineNumber}:${xpp.columnNumber - 2}@$entry"

        fun openStream(path: String): InputStream {
            entry = reader.getEntry(path) ?: fail("pmab.parse.notFoundFile", path, reader.name)
            return reader.getInputStream(entry)
        }

        fun newChapter() {
            chapter = chapter.newChapter()
        }
    }
}
