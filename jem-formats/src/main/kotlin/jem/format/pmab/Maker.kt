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
import jclp.io.Flob
import jclp.io.extName
import jclp.setting.Settings
import jclp.setting.getString
import jclp.text.ConverterManager
import jclp.text.TEXT_PLAIN
import jclp.text.Text
import jclp.vdm.*
import jem.Book
import jem.Chapter
import jem.epm.EXTENSION_FILE_INFO
import jem.epm.VdmMaker
import jem.format.util.XmlRender
import jem.format.util.failMaker
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

internal object PmabMaker : VdmMaker {
    override fun make(book: Book, output: VdmWriter, arguments: Settings?) {
        val data = Local(book, output, arguments)
        val version = data.getConfig("version")
        when (version) {
            null, "3.0" -> writePMABv3(data)
            else -> failMaker("pmab.make.unsupportedVersion", version)
        }
    }

    private fun writePMABv3(data: Local) {
        data.writer.writeBytes(PMAB.MIME_PATH, PMAB.MIME_PMAB.toByteArray())
        writePBMv3(data)
        writePBCv3(data)
    }

    private fun writePBMv3(data: Local) {
        data.beginXml("pbm", "3.0", PMAB.PBM_XMLNS).let { out ->
            writeMetadata(data)
            writeItems(data.book.attributes, "attributes", "m-", data)
            writeItems(data.book.extensions, "extensions", "x-", data)
            data.endXml(out, PMAB.PBM_PATH)
        }
    }

    private fun writePBCv3(data: Local) {
        data.beginXml("pbc", "3.0", PMAB.PBC_XMLNS).let { out ->
            with(data.render) {
                beginTag("nav")
                data.book.forEachIndexed { i, chapter ->
                    writeChapter(chapter, (i + 1).toString(), data)
                }
                endTag()
            }
            data.endXml(out, PMAB.PBC_PATH)
        }
    }

    private fun writeChapter(chapter: Chapter, suffix: String, data: Local) {
        with(data.render) {
            beginTag("chapter")
            val prefix = "chapter-$suffix"
            writeItems(chapter.attributes, "attributes", prefix + "-", data)
            chapter.text?.let {
                beginTag("content")
                attribute("type", textType(it, data))
                text(writeText(it, prefix, data))
                endTag()
            }
            chapter.forEachIndexed { i, chapter ->
                writeChapter(chapter, "$suffix-${i + 1}", data)
            }
            endTag()
        }
    }

    private fun writeMetadata(data: Local) {
        val values = data.arguments?.get("maker.pmab.meta") as? Map<*, *> ?: return
        with(data.render) {
            beginTag("head")
            for ((key, value) in values) {
                beginTag("meta")
                attribute("name", key?.toString() ?: "")
                attribute("value", value?.toString() ?: "")
                endTag()
            }
            endTag()
        }
    }

    private fun writeItems(map: ValueMap, tag: String, prefix: String, data: Local) {
        with(data.render) {
            beginTag(tag)
            for ((name, value) in map) {
                if (!name.startsWith("!--") && name != EXTENSION_FILE_INFO) {
                    writeItem(name, value, prefix, data)
                }
            }
            endTag()
        }
    }

    private fun writeItem(name: String, value: Any, prefix: String, data: Local) {
        with(data.render) {
            beginTag("item")
            attribute("name", name)
            var type: String = TypeManager.STRING
            val text = when (value) {
                is Text -> {
                    type = textType(value, data)
                    writeText(value, prefix + name, data)
                }
                is Flob -> {
                    type = value.mimeType
                    writeFlob(value, prefix + name, data)
                }
                is CharSequence -> {
                    type = TypeManager.STRING
                    value.toString()
                }
                is LocalDate -> (data.getConfig("dateFormat") ?: DATE_FORMAT).let {
                    type = "${TypeManager.DATE};format=$it"
                    value.format(DateTimeFormatter.ofPattern(it))
                }
                is LocalTime -> (data.getConfig("timeFormat") ?: TIME_FORMAT).let {
                    type = "${TypeManager.TIME};format=$it"
                    value.format(DateTimeFormatter.ofPattern(it))
                }
                is LocalDateTime -> (data.getConfig("datetimeFormat") ?: DATE_TIME_FORMAT).let {
                    type = "${TypeManager.DATETIME};format=$it"
                    value.format(DateTimeFormatter.ofPattern(it))
                }
                else -> {
                    type = TypeManager.getType(value) ?: TypeManager.STRING
                    ConverterManager.render(value) ?: value.toString()
                }
            }
            attribute("type", type)
            text(text ?: value.toString())
            endTag()
        }
    }

    private fun writeFlob(flob: Flob, name: String, data: Local): String {
        val path = "resources/$name.${extName(flob.name).takeIf { it.isNotEmpty() } ?: "dat"}"
        data.writer.writeFlob(path, flob)
        return path
    }

    private fun textType(text: Text, data: Local) = "text/${text.type};encoding=${data.charset}"

    private fun writeText(text: Text, name: String, data: Local): String {
        val path = "text/$name.${if (text.type == TEXT_PLAIN) "txt" else text.type}"
        data.writer.writeText(path, text, data.charset)
        return path
    }

    private data class Local(val book: Book, val writer: VdmWriter, val arguments: Settings?) {
        val render = XmlRender(arguments)

        val charset: Charset =
                getConfig("encoding")?.let { Charset.forName(it) } ?: Charset.defaultCharset()

        fun getConfig(key: String) = arguments?.getString("maker.pmab.$key")

        fun beginXml(root: String, version: String, xmlns: String) = ByteArrayOutputStream().also {
            with(render) {
                output(it)
                beginXml()
                beginTag(root)
                attribute("version", version)
                xmlns(xmlns)
            }
        }

        fun endXml(buffer: ByteArrayOutputStream, path: String) {
            render.endTag().endXml()
            writer.useStream(path) { buffer.writeTo(it) }
        }
    }
}
