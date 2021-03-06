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

import jclp.*
import jclp.io.flobOf
import jclp.setting.Settings
import jclp.setting.getString
import jclp.text.*
import jclp.vdm.VdmEntry
import jclp.vdm.VdmReader
import jclp.vdm.readText
import jclp.xml.getAttribute
import jem.*
import jem.epm.EXT_EPM_METADATA
import jem.epm.VdmParser
import jem.format.util.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.nio.charset.Charset

internal object PmabParser : VdmParser {
    override fun parse(input: VdmReader, arguments: Settings?) = if (input.readText(PMAB.MIME_PATH)?.trim() != PMAB.MIME_PMAB) {
        failParser("pmab.parse.badMime", PMAB.MIME_PATH, PMAB.MIME_PMAB)
    } else {
        Book().also {
            val data = Local(it, input, arguments)
            try {
                parsePBM(data)
                parsePBC(data)
            } catch (e: Exception) {
                it.cleanup()
                throw e
            }
            it.extensions[EXT_EPM_METADATA] = data.meta
        }
    }

    private fun parsePBM(data: Local) {
        var version = 0
        val xpp = data.newXpp()
        data.openStream(PMAB.PBM_PATH).use {
            xpp.setInput(it, null)
            useXmlLoop(xpp, PMAB.PBM_PATH) { begin, buf ->
                if (begin) {
                    var useText = false
                    when {
                        version == 3 -> useText = beginPBMv3(xpp.name, data)
                        version != 0 -> TODO("PBM v$version is not implemented")
                        xpp.name == "pbm" -> version = getVersion(xpp, "pmab.parse.unsupportedPBM", data)
                    }
                    useText
                } else {
                    when (version) {
                        3 -> endPBMv3(xpp.name, buf, data)
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
            useXmlLoop(xpp, PMAB.PBC_PATH) { start, buf ->
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
                        3 -> endPBCv3(xpp.name, buf, data)
                    }
                    false
                }
            }
        }
    }

    private fun beginPBMv3(tagName: String, data: Local): Boolean {
        val xpp = data.xpp
        var useText = false
        when (tagName) {
            "item" -> {
                data.itemName = data.xmlAttribute("name")
                data.itemType = xpp.getAttribute("type")
                useText = true
            }
            "attributes" -> data.values = data.book.attributes
            "extensions" -> data.values = data.book.extensions
            "meta" -> data.meta[data.xmlAttribute("name")] = data.xmlAttribute("value")
        }
        return useText
    }

    private fun endPBMv3(tagName: String, buf: StringBuilder, data: Local) {
        if (tagName == "item") {
            parseItem(buf.toString().trim(), data).let {
                data.values[data.itemName] = it
                it.release()
            }
        }
    }

    private fun beginPBCv3(tagName: String, data: Local): Boolean {
        val xpp = data.xpp
        var useText = false
        when (tagName) {
            "item" -> {
                data.itemName = data.xmlAttribute("name")
                data.itemType = xpp.getAttribute("type")
                useText = true
            }
            "chapter" -> data.newChapter()
            "content" -> {
                data.itemType = xpp.getAttribute("type")
                useText = true
            }
        }
        return useText
    }

    private fun endPBCv3(tagName: String, buf: StringBuilder, data: Local) {
        when (tagName) {
            "chapter" -> data.chapter = data.chapter.parent!!
            "item" -> parseItem(buf.toString().trim(), data).let {
                data.chapter[data.itemName] = it
                it.release()
            }
            "content" -> (parseItem(buf.toString().trim(), data) as Text).let {
                data.chapter.text = it
                it.release()
            }
        }
    }

    private fun fallbackType(name: String, type: String?): String {
        if (type.isNullOrEmpty() || type == TypeManager.STRING) {
            Attributes.getType(name)?.let { return it }
        }
        return when (name) {
            WORDS -> TypeManager.STRING
            else -> type or TypeManager.STRING
        }
    }

    private fun parseItem(text: String, data: Local): Any {
        val itemType = fallbackType(data.itemName, data.itemType)
        val parts = itemType.split(';')
        val type = parts.first()
        return when (type) {
            TypeManager.STRING, "" -> text
            TypeManager.REAL -> parseDouble(text) { data.xmlPosition() }
            TypeManager.BOOLEAN -> text.toBoolean()
            TypeManager.INTEGER, "uint" -> parseInt(text) { data.xmlPosition() }
            TypeManager.LOCALE -> parseLocale(text)
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
                    managed(flobOf(data.reader, text, type)) {
                        textOf(it, encoding?.let { Charset.forName(it) }, type.substring(5))
                    }
                }
                type.matches("[\\w]+/.+".toRegex()) -> flobOf(data.reader, text, type)
                else -> TypeManager.getClass(type)?.let { ConverterManager.parse(text, it) } ?: text
            }
        }
    }

    private fun getVersion(xpp: XmlPullParser, error: String, data: Local) = data.xmlAttribute("version").let {
        data.meta["version"] = it
        when (it) {
            "3.0" -> 3
            "2.0" -> 2
            else -> failParser(error, it, xpp.lineNumber, data.entry)
        }
    }

    private data class Local(val book: Book, val reader: VdmReader, val arguments: Settings?) {
        val meta = hashMapOf<String, String>()

        var chapter: Chapter = book

        var itemType: String? = null

        lateinit var itemName: String

        lateinit var values: ValueMap

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
            entry = reader.getEntry(path) ?: failParser("pmab.parse.notFoundFile", path, reader.name)
            return reader.getInputStream(entry)
        }

        fun newChapter() {
            chapter = chapter.newChapter()
        }
    }
}
