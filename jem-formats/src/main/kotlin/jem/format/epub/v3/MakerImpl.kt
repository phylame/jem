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

package jem.format.epub.v3

import jclp.io.Flob
import jclp.setting.Settings
import jclp.text.Text
import jclp.text.ifNotEmpty
import jclp.vdm.VdmWriter
import jclp.vdm.useStream
import jclp.vdm.writeFlob
import jclp.xml.Tag
import jclp.xml.newSerializer
import jclp.xml.startDocument
import jclp.xml.xml
import jem.*
import jem.format.util.M
import jem.format.util.xmlEncoding
import jem.format.util.xmlIndent
import jem.format.util.xmlSeparator
import java.io.InterruptedIOException
import java.nio.charset.Charset
import java.nio.file.Paths
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZoneOffset
import java.util.*

private class Local(val book: Book, val writer: VdmWriter, val settings: Settings?) {
    val encoding = settings.xmlEncoding

    val charset: Charset = Charset.forName(encoding)

    val indent = settings.xmlIndent

    val lineSeparator = settings.xmlSeparator

    val opsDir = getConfig("opsDir") ?: "EPUB"

    val styleDir = getConfig("styleDir") ?: "Styles"

    val imageDir = getConfig("imageDir") ?: "Images"

    val textDir = getConfig("textDir") ?: "Text"

    val textRoot = Paths.get(textDir)

    val lang: String =
            getConfig("language") ?: book.language?.toLanguageTag() ?: Locale.getDefault().toLanguageTag()

    var css: String = ""

    var mask: String = ""

    val pkg = Package()

    inline fun <reified T : Any> getConfig(name: String) = settings?.get("maker.epub.$name", T::class.java)
}

internal fun makeImpl(book: Book, writer: VdmWriter, settings: Settings?) {
    val data = Local(book, writer, settings)

    renderMeta(data)
    renderNav(data)

    writer.useStream("${data.opsDir}/package.opf") {
        newSerializer(it, data.encoding, data.indent, data.lineSeparator).apply {
            startDocument(data.encoding)
            data.pkg.renderTo(this)
            flush()
        }
    }
}

private fun renderMeta(data: Local) {
    val book = data.book
    val md = data.pkg.metadata

    data.pkg.language = data.lang

    md.addTitle(book.title)
    book.author.split(Attributes.VALUE_SEPARATOR).forEach {
        md.addAuthor(it)
    }
    md.addDCME("language", data.lang)
    (book[PUBDATE] as? LocalDate)?.let {
        md.addPubdate(it.atTime(OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC)))
    }
}

private fun renderNav(data: Local) {
    val book = data.book

    data.css = "../Styles/main.css"
    data.mask = "../Images/mask.png"

    book.cover?.let {
        renderCover(it, data)
    }

    book.intro?.ifNotEmpty {
        renderIntro(it, data)
    }

    // nav
    data.pkg.spine.addReference("nav", false)
    book.forEachIndexed { index, chapter ->
        if (Thread.interrupted()) throw InterruptedIOException()
        renderToc(chapter, "-${index + 1}", data)
    }
}

private fun renderToc(chapter: Chapter, suffix: String, data: Local) {
    val id = if (chapter.isSection) "section$suffix" else "chapter$suffix"

    if (chapter.isSection) {
        renderSection(id, chapter, data)
    } else {
        renderChapter(id, chapter, data)
    }

    chapter.forEachIndexed { index, stub ->
        if (Thread.interrupted()) throw InterruptedIOException()
        renderToc(stub, "$suffix-${index + 1}", data)
    }
}

private fun renderCover(cover: Flob, data: Local) {
    renderImage(cover, "cover", M.tr("epub.make.coverTitle"), data.book.title, data)
}

private fun renderIntro(intro: Text, data: Local) {
    val title = M.tr("epub.make.introTitle")
    renderPage(title, "intro", data) {
        tag("div") {
            attr["class"] = "main intro"
            tag("h1", title)
            intro.forEach { tag("p", it.trim()) }
        }
    }
}

private fun renderSection(id: String, section: Chapter, data: Local) {
    val cover = section.cover
    val intro = section.intro

    if (cover == null && intro?.isEmpty() != false) return

    val imageName = cover?.let {
        writeImage(it, "$id-cover", data).second
    }

    renderPage(section.title, id, data) {
        if (imageName != null) {
            attr["style"] = "background-image: url(${"../${data.imageDir}/$imageName"});background-size: cover;"
            intro?.ifNotEmpty {
                tag("div") {
                    attr["style"] = "background: url(${data.mask}) repeat;"
                    attr["class"] = "main section"
                    it.forEach { tag("p", it.trim()) }
                }
            }
        } else {
            intro?.ifNotEmpty {
                tag("div") {
                    attr["class"] = "main section"
                    tag("h1", section.title)
                    it.forEach { tag("p", it.trim()) }
                }
            }
        }
    }
}

private fun renderChapter(id: String, chapter: Chapter, data: Local) {
    val cover = chapter.cover
    val intro = chapter.intro
    val text = chapter.text

    if (cover != null) {
        renderImage(cover, "$id-cover", chapter.title, chapter.title, data)
    }

    if (intro?.isEmpty() != false && text?.isEmpty() != false) return

    renderPage(chapter.title, id, data) {
        tag("div") {
            attr["class"] = "main chapter"
            tag("h1", chapter.title)
            intro?.ifNotEmpty {
                tag("section") {
                    attr["class"] = "brief"
                    it.forEach { tag("p", it.trim()) }
                    tag("hr")
                }
            }
            if (text != null) {
                tag("section") {
                    attr["class"] = "text"
                    text.forEach { tag("p", it.trim()) }
                }
            }
        }
    }
}

private inline fun renderPage(title: String, id: String, data: Local, block: Tag.() -> Unit) {
    val opsPath = "${data.textDir}/$id.xhtml"
    data.writer.useStream("${data.opsDir}/$opsPath") {
        it.bufferedWriter(data.charset).xml(data.indent, data.lineSeparator, data.encoding) {
            doctype("html")
            tag("html") {
                attr["xmlns"] = "http://www.w3.org/1999/xhtml"
                attr["lang"] = data.lang
                tag("head") {
                    tag("meta") {
                        attr["charset"] = data.encoding
                    }
                    tag("title", title)
                    tag("link") {
                        attr["rel"] = "stylesheet"
                        attr["href"] = data.css
                    }
                }
                tag("body", block)
            }
        }
    }
    data.pkg.manifest.addItem(Resource(id, opsPath, "application/xhtml+xml"))
    data.pkg.spine.addReference(id)
}

private fun renderImage(image: Flob, id: String, title: String, alt: String, data: Local) {
    val imageName = writeImage(image, id, data).second
    renderPage(title, "$id-page", data) {
        tag("div") {
            attr["class"] = "main cover"
            tag("img") {
                attr["src"] = "../${data.imageDir}/$imageName"
                attr["alt"] = alt
            }
        }
    }
}

private fun writeImage(image: Flob, name: String, data: Local): Pair<String, String> {
    val suffix = image.mimeType.removePrefix("image/").let {
        when (it) {
            "jpeg" -> "jpg"
            else -> it
        }
    }
    val fullName = "$name.$suffix"
    val opsPath = "${data.imageDir}/$fullName"
    val entryName = "${data.opsDir}/$opsPath"
    data.writer.writeFlob(entryName, image)
    if (name == "cover") {
        data.pkg.manifest.addCoverImage("cover", opsPath, image.mimeType)
    } else {
        data.pkg.manifest.addItem(Resource(name, opsPath, image.mimeType))
    }
    return entryName to fullName
}
