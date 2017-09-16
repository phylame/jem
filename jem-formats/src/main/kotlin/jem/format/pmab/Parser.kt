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

package jem.format.pmab

import jclp.Types
import jclp.VariantMap
import jclp.flob.Flob
import jclp.setting.Settings
import jclp.setting.getString
import jclp.text.Text
import jclp.text.valueFor
import jclp.vdm.VDMEntry
import jclp.vdm.VDMReader
import jclp.vdm.readText
import jem.Attributes
import jem.Book
import jem.Chapter
import jem.epm.VDMParser
import jem.format.util.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.nio.file.NoSuchFileException
import java.util.*

internal object PmabParser : VDMParser {
    override fun parse(input: VDMReader, arguments: Settings?) = if (input.readText(MIME_PATH) != MIME_PMAB) {
        fail("pmab.parse.badMime", MIME_PATH, MIME_PMAB)
    } else Book().apply {
        val data = Local(this, input, arguments)
        parsePBM(data)
        parsePBC(data)
    }

    private fun parsePBM(data: Local) {
        var version = 0
        val xpp = data.newXpp()
        data.openStream(PBM_PATH).use {
            xpp.setInput(it, null)
            useXml(xpp, PBM_PATH) { start, sb ->
                if (start) {
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
        data.openStream(PBC_PATH).use {
            xpp.setInput(it, null)
            useXml(xpp, PBC_PATH) { start, sb ->
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
                data.itemName = data.getAttribute("name")
                data.itemType = xpp.getAttributeValue(null, "type")
                hasText = true
            }
            "attributes" -> data.values = data.book.attributes
            "extensions" -> data.values = data.book.extensions
            "meta" -> data.meta[data.getAttribute("name")] = data.getAttribute("value")
            "head" -> data.meta = HashMap()
        }
        return hasText
    }

    private fun endPBMv3(tag: String, sb: StringBuilder, data: Local) {
        if (tag == "item") {
            data.values[data.itemName] = parseItem(sb.toString().trim(), data)
        }
    }

    private fun beginPBCv3(tag: String, data: Local): Boolean {
        val xpp = data.xpp
        var hasText = false
        when (tag) {
            "item" -> {
                data.itemName = data.getAttribute("name")
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
            "item" -> data.chapter[data.itemName] = parseItem(sb.toString().trim(), data)
            "content" -> data.chapter.text = parseItem(sb.toString().trim(), data) as Text
        }
    }

    private fun parseItem(str: String, data: Local): Any {
        val itemType = data.itemType ?: Attributes.getType(data.itemName) ?: return str
        val parts = itemType.split(';')
        val type = parts.first()
        return when (type) {
            Types.STRING, "" -> str
            Types.REAL -> parseDouble(str) { data.xmlPosition() }
            Types.BOOLEAN -> str.toBoolean()
            Types.INTEGER, "unit" -> parseInt(str) { data.xmlPosition() }
            Types.LOCALE -> Locale.forLanguageTag(str.replace('_', '-'))
            Types.DATETIME -> {
                val format = data.getConfig("datetimeFormat") ?: itemType.valueFor("format") ?: ""
                parseDateTime(str, format) { data.xmlPosition() }
            }
            Types.DATE -> {
                val format = data.getConfig("dateFormat") ?: itemType.valueFor("format") ?: ""
                parseDate(str, format) { data.xmlPosition() }
            }
            Types.TIME -> {
                val format = data.getConfig("timeFormat") ?: itemType.valueFor("format") ?: ""
                parseTime(str, format) { data.xmlPosition() }
            }
            else -> when {
                type.startsWith("text/") -> {
                    val encoding = data.getConfig("encoding") ?: itemType.valueFor("encoding")
                    Text.of(Flob.of(data.reader, str, type), encoding, type.substring(5))
                }
                type.matches("[a-z]+/[a-z]+".toRegex()) -> Flob.of(data.reader, str, type)
                else -> str
            }
        }
    }

    private fun getVersion(xpp: XmlPullParser, msg: String, data: Local) = xmlAttribute(xpp, "version", data.entry).let {
        when (it) {
            "3.0" -> 3
            "2.0" -> 2
            else -> fail(msg, it, xpp.lineNumber, data.entry)
        }
    }

    private data class Local(val book: Book, val reader: VDMReader, val arguments: Settings?) {
        var chapter: Chapter = book

        var itemType: String? = null

        lateinit var itemName: String

        lateinit var values: VariantMap

        lateinit var meta: MutableMap<String, String>

        lateinit var xpp: XmlPullParser

        lateinit var entry: VDMEntry

        fun newXpp(): XmlPullParser {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            xpp = factory.newPullParser()
            return xpp
        }

        fun getAttribute(name: String) = xmlAttribute(xpp, name, entry)

        fun getConfig(key: String) = arguments?.getString("parser.pmab.$key")

        fun xmlPosition() = "${xpp.lineNumber}:${xpp.columnNumber - 2}@$entry"

        fun openStream(path: String): InputStream {
            entry = reader.getEntry(path) ?: throw NoSuchFileException(M.tr("pmab.parse.notFoundFile", path, reader.name))
            return reader.getInputStream(entry)
        }

        fun newChapter() {
            chapter = chapter.newChapter()
        }
    }
}
