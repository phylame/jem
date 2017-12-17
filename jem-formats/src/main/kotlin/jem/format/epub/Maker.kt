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

package jem.format.epub

import jclp.io.Flob
import jclp.io.flobOf
import jclp.setting.Settings
import jclp.setting.getString
import jclp.vdm.VdmWriter
import jclp.vdm.writeBytes
import jclp.vdm.writeFlob
import jem.Book
import jem.epm.VdmMaker
import jem.format.epub.html.Nav
import jem.format.epub.opf.Package
import jem.format.epub.opf.Resource
import jem.format.epub.v2.makeImpl201
import jem.format.epub.v3.makeImpl301
import jem.format.util.*
import jem.language
import org.xmlpull.v1.XmlSerializer
import java.util.*
import jem.format.util.M as T

internal object EpubMaker : VdmMaker {
    private val versions = arrayOf("", "2", "2.0", "2.0.1", "3", "3.0", "3.0.1")

    override fun validate(book: Book, output: String, arguments: Settings?) {
        (arguments?.getString("maker.epub.version") ?: "").let {
            if (it !in versions) {
                failMaker("epub.make.unsupportedVersion", it)
            }
        }
    }

    override fun make(book: Book, output: VdmWriter, arguments: Settings?) {
        output.writeBytes(EPUB.MIME_PATH, EPUB.MIME_EPUB.toByteArray())
        val version = arguments?.getString("maker.epub.version") ?: ""
        val data = when (version) {
            "", "2", "2.0", "2.0.1" -> makeImpl201(book, output, arguments)
            "3", "3.0", "3.0.1" -> makeImpl301(book, output, arguments)
            else -> throw InternalError()
        }
        val path = "${data.opsDir}/package.opf"
        output.xmlSerializer(path, arguments) { data.pkg.renderTo(this) }
        writeContainer(output, arguments, mapOf(path to EPUB.MIME_OPF))
    }
}

fun writeContainer(writer: VdmWriter, settings: Settings?, files: Map<String, String>) {
    writer.xmlDsl("META-INF/container.xml", settings) {
        tag("container") {
            attr["version"] = "1.0"
            attr["xmlns"] = "urn:oasis:names:tc:opendocument:xmlns:container"
            tag("rootfiles") {
                for ((path, mime) in files) {
                    tag("rootfile") {
                        attr["full-path"] = path
                        attr["media-type"] = mime
                    }
                }
            }
        }
    }
}

interface Renderable {
    fun renderTo(xml: XmlSerializer)
}

abstract class Taggable(var id: String) : Renderable {
    val attr = linkedMapOf<String, String>()
}

internal open class DataHolder(version: String, val book: Book, val writer: VdmWriter, val settings: Settings?) {
    var nav = Nav("")

    val pkg = Package(version)

    var bookId = ""

    var epubVersion = 4

    var maskHref = ""

    var cssHref = ""

    val encoding = settings.xmlEncoding

    val separator = settings.xmlSeparator

    val opsDir = getConfig("opsDir") ?: "EPUB"

    val textDir = getConfig("textDir") ?: "Text"

    val styleDir = getConfig("styleDir") ?: "Styles"

    val imageDir = getConfig("imageDir") ?: "Images"

    val extraDir = getConfig("extraDir") ?: "Extras"

    val useDuokanCover = getConfig("useDuokanCover") ?: true

    val cssPath = getConfig("cssPath") ?: "!jem/format/epub/main.css"

    val maskPath = getConfig("maskPath") ?: "!jem/format/epub/mask.png"

    val lang: String = getConfig("language") ?: (book.language ?: Locale.getDefault()).toLanguageTag()

    inline fun <reified T : Any> getConfig(name: String): T? = settings?.get("maker.epub.$name", T::class.java)

    fun writeResource(path: String, id: String) = writeFlob(flobOf(path, javaClass.classLoader), id)

    fun writeFlob(flob: Flob, id: String): Resource {
        val mime = flob.mimeType
        val dir = when {
            mime == EPUB.MIME_CSS -> styleDir
            mime.startsWith("image/") -> imageDir
            "html" in mime || mime.startsWith("text/") -> textDir
            else -> extraDir
        }
        val ext = mime.split('/').last().let {
            when (it) {
                "jpeg" -> "jpg"
                else -> it
            }
        }
        val opsPath = "$dir/$id.$ext"
        writer.writeFlob("$opsDir/$opsPath", flob)
        return pkg.manifest.addResource(id, opsPath, mime).also {
            if (epubVersion == 3 && id == "cover") {
                it.attr["properties"] = EPUB.MANIFEST_COVER_IMAGE
            }
        }
    }
}
