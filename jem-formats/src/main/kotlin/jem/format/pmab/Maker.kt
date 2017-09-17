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

import jclp.*
import jclp.flob.Flob
import jclp.io.extName
import jclp.setting.Settings
import jclp.setting.getString
import jclp.text.TEXT_PLAIN
import jclp.text.Text
import jclp.vdm.VDMWriter
import jclp.vdm.useStream
import jclp.vdm.write
import jem.Book
import jem.Chapter
import jem.epm.VDMMaker
import jem.format.util.XmlRender
import jem.format.util.fail
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

internal object PmabMaker : VDMMaker {
    override fun make(book: Book, output: VDMWriter, arguments: Settings?) {
        val data = Local(book, output, arguments)
        val version = data.getConfig("version")
        when (version) {
            null, "3.0" -> writePMABv3(data)
            else -> fail("pmab.make.unsupportedVersion", version)
        }
    }

    private fun writePMABv3(data: Local) {
        data.writer.useStream(MIME_PATH) {
            write(MIME_PMAB.toByteArray())
        }
        writePBMv3(data)
        writePBCv3(data)
    }

    private fun writePBMv3(data: Local) {
        data.beginXml("pbm", "3.0", PBM_XMLNS).let {
            writeMetadata(data)
            writeItems(data.book.attributes, "attributes", "m-", data)
            writeItems(data.book.extensions, "extensions", "x-", data)
            data.endXml(it, PBM_PATH)
        }
    }

    private fun writePBCv3(data: Local) {
        data.beginXml("pbc", "3.0", PBC_XMLNS).let {
            with(data.render) {
                beginTag("nav")
                data.book.forEachIndexed { i, chapter ->
                    writeChapter(chapter, (i + 1).toString(), data)
                }
                endTag()
            }
            data.endXml(it, PBC_PATH)
        }
    }

    private fun writeChapter(chapter: Chapter, suffix: String, data: Local) {
        with(data.render) {
            beginTag("chapter")
            val prefix = "chapter-$suffix"
            writeItems(chapter.attributes, "attributes", prefix + "-", data)
            chapter.text?.let {
                beginTag("content")
                        .attribute("type", typeOf(it, data))
                        .text(writeText(it, prefix, data))
                        .endTag()
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
                        .attribute("name", key?.toString() ?: "")
                        .attribute("value", value?.toString() ?: "")
                        .endTag()
            }
            endTag()
        }
    }

    private fun writeItems(map: VariantMap, tag: String, prefix: String, data: Local) {
        with(data.render) {
            beginTag(tag)
            for ((name, value) in map) {
                writeItem(name, value, prefix, data)
            }
            endTag()
        }
    }

    private fun writeItem(name: String, value: Any, prefix: String, data: Local) {
        with(data.render) {
            beginTag("item").attribute("name", name)
            var type = Variants.getType(value) ?: Variants.STRING
            val text = when (type) {
                Variants.TEXT -> (value as Text).let {
                    type = typeOf(it, data)
                    writeText(it, prefix + name, data)
                }
                Variants.FLOB -> (value as Flob).let {
                    type = it.mime
                    writeFlob(it, prefix + name, data)
                }
                Variants.DATE -> (data.getConfig("dateFormat") ?: LOOSE_DATE_FORMAT).let {
                    type += ";format=" + it
                    (value as LocalDate).format(DateTimeFormatter.ofPattern(it))
                }
                Variants.TIME -> (data.getConfig("timeFormat") ?: LOOSE_TIME_FORMAT).let {
                    type += ";format=" + it
                    (value as LocalTime).format(DateTimeFormatter.ofPattern(it))
                }
                Variants.DATETIME -> (data.getConfig("datetimeFormat") ?: LOOSE_DATE_TIME_FORMAT).let {
                    type += ";format=" + it
                    (value as LocalDateTime).format(DateTimeFormatter.ofPattern(it))
                }
                else -> null
            }
            attribute("type", type).text(text ?: value.toString()).endTag()
        }
    }

    private fun writeFlob(flob: Flob, name: String, data: Local): String {
        val path = "resources/$name.${extName(flob.name).takeIf(String::isNotEmpty) ?: "dat"}"
        data.writer.write(path, flob)
        return path
    }

    private fun typeOf(text: Text, data: Local) = "text/${text.type};encoding=${data.charset}"

    private fun writeText(text: Text, name: String, data: Local): String {
        val path = "text/$name.${if (text.type == TEXT_PLAIN) "txt" else text.type}"
        data.writer.write(path, text, data.charset)
        return path
    }

    private data class Local(val book: Book, val writer: VDMWriter, val arguments: Settings?) {
        val render = XmlRender(arguments)

        val charset: Charset = getConfig("encoding")?.let {
            Charset.forName(it)
        } ?: Charset.defaultCharset()

        fun getConfig(key: String) = arguments?.getString("maker.pmab.$key")

        fun beginXml(root: String, version: String, xmlns: String) = ByteArrayOutputStream().apply {
            render.output(this)
                    .beginXml()
                    .beginTag(root)
                    .attribute("version", version)
                    .xmlns(xmlns)
        }

        fun endXml(buffer: ByteArrayOutputStream, path: String) {
            render.endTag().endXml()
            writer.useStream(path, buffer::writeTo)
        }
    }
}
