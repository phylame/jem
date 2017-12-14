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
import jclp.io.flobOf
import jclp.isNotRoot
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
import jem.format.epub.EPUB
import jem.format.epub.writeContainer
import jem.format.util.M
import jem.format.util.xmlEncoding
import jem.format.util.xmlIndent
import jem.format.util.xmlSeparator
import java.io.InterruptedIOException
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.OffsetDateTime
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

    val extraDir = getConfig("extraDir") ?: "Extras"

    val textDir = getConfig("textDir") ?: "Text"

    val lang: String =
            getConfig("language") ?: book.language?.toLanguageTag() ?: Locale.getDefault().toLanguageTag()

    var css: String = ""

    var mask: String = ""

    val pkg = Package()

    var nav = Nav("")

    inline fun <reified T : Any> getConfig(name: String) = settings?.get("maker.epub.$name", T::class.java)
}

private class Nav(val title: String, val href: String = "") {
    var parent: Nav? = null

    val items = LinkedList<Nav>()

    fun newNav(title: String, href: String) {
        items += Nav(title, href).also { it.parent = this }
    }
}

internal fun makeImpl(book: Book, writer: VdmWriter, settings: Settings?) {
    val data = Local(book, writer, settings)

    renderMeta(data)
    renderNav(data)

    val opfPath = "${data.opsDir}/package.opf"
    writer.useStream(opfPath) {
        newSerializer(it, data.encoding, data.indent, data.lineSeparator).apply {
            startDocument(data.encoding)
            data.pkg.renderTo(this)
            flush()
        }
    }

    writeContainer(writer, settings, mapOf(opfPath to EPUB.MIME_OPF))
}

private fun renderMeta(data: Local) {
    val pkg = data.pkg
    val md = pkg.metadata
    val book = data.book

    pkg.language = data.lang

    book[ISBN]?.toString()?.ifNotEmpty {
        md.addISBN("isbn", it)
        pkg.uniqueIdentifier = "isbn"
    }
    book["uuid"]?.toString()?.ifNotEmpty {
        md.addISBN("uuid", it)
        pkg.uniqueIdentifier = "uuid"
    }
    if (pkg.uniqueIdentifier.isEmpty()) {
        md.addUUID("uuid", UUID.randomUUID().toString())
        pkg.uniqueIdentifier = "uuid"
    }
    md.addModifiedTime(OffsetDateTime.now())

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

    val cssPath = writeResource(data.getConfig("cssPath") ?: "!jem/format/epub/v3/main.css", "style", data, "")
    data.css = "../$cssPath"

    val maskPath = writeResource(data.getConfig("maskPath") ?: "!jem/format/epub/v3/mask.png", "mask", data, "")
    data.mask = "../$maskPath"

    book.cover?.let {
        renderCover(it, data)
    }

    book.intro?.ifNotEmpty {
        renderIntro(it, data)
    }

    // nav
    val title = M.tr("epub.make.navTitle")
    data.pkg.spine.addReference("nav", false)
    data.nav.newNav(title, "nav.xhtml")

    renderText(book, "", data)

    renderPage(title, "nav", data) {
        tag("div") {
            attr["class"] = "main toc"
            tag("h1", title)
            tag("nav") {
                attr["epub:type"] = "toc"
                tag("ol") {
                    makeNav(data.nav, this)
                }
            }
        }
    }
}

private fun makeNav(nav: Nav, ol: Tag) {
    with(nav) {
        if (title.isNotEmpty()) { // not root
            ol.tag("li") {
                if (href.isNotEmpty()) {
                    tag("a") {
                        attr["href"] = href
                        +title
                    }
                } else {
                    tag("span", title)
                }
                if (items.isNotEmpty()) {
                    tag("ol") {
                        items.forEach { makeNav(it, this) }
                    }
                }
            }
        } else {
            items.forEach { makeNav(it, ol) }
        }
    }
}

private fun renderText(chapter: Chapter, suffix: String, data: Local) {
    val isSection = chapter.isSection
    if (chapter.isNotRoot) {
        val id = if (isSection) "section$suffix" else "chapter$suffix"
        if (isSection) {
            renderSection(id, chapter, data)
            data.nav = data.nav.items.last
        } else {
            renderChapter(id, chapter, data)
        }
    }
    if (isSection) {
        chapter.forEachIndexed { index, stub ->
            if (Thread.interrupted()) throw InterruptedIOException()
            renderText(stub, "$suffix-${index + 1}", data)
        }
        data.nav.parent?.let { data.nav = it }
    }
}

private fun renderCover(cover: Flob, data: Local) {
    renderImage(cover, EPUB.COVER_ID, M.tr("epub.make.coverTitle"), data.book.title, data)
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
    val title = section.title

    if (cover == null && intro?.isEmpty() != false) {
        data.nav.newNav(title, "")
        return
    }

    val imageName = cover?.let {
        writeFlob(it, "$id-cover", data, "")
    }

    renderPage(title, id, data) {
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
                    tag("h1", title)
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
    val title = chapter.title

    if (cover != null) {
        renderImage(cover, "$id-cover", title, title, data)
    }

    if (intro?.isEmpty() != false && text?.isEmpty() != false) {
        data.nav.newNav(title, "")
        return
    }

    renderPage(title, id, data) {
        tag("div") {
            attr["class"] = "main chapter"
            tag("h1", title)
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
    with(data) {
        val opsPath = "$textDir/$id.xhtml"
        writer.useStream("$opsDir/$opsPath") {
            it.bufferedWriter(charset).xml(indent, lineSeparator, encoding) {
                doctype("html")
                tag("html") {
                    attr["xmlns"] = "http://www.w3.org/1999/xhtml"
                    attr["xmlns:epub"] = "http://www.idpf.org/2007/ops"
                    attr["lang"] = lang
                    tag("head") {
                        tag("meta") {
                            attr["charset"] = encoding
                        }
                        tag("title", title)
                        tag("link") {
                            attr["rel"] = "stylesheet"
                            attr["type"] = "text/css"
                            attr["href"] = css
                        }
                    }
                    tag("body", block)
                }
            }
        }
        nav.newNav(title, "$id.xhtml")
        if (id == "nav") {
            pkg.manifest.addNavigation("nav", opsPath)
        } else {
            pkg.manifest.addItem(Resource(id, opsPath, EPUB.MIME_XHTML))
        }
        pkg.spine.addReference(id, properties = if ("cover" in id) EPUB.DUOKAN_FULLSCREEN else "")
    }
}

private fun renderImage(image: Flob, id: String, title: String, alt: String, data: Local) {
    val opsPath = writeFlob(image, id, data, if (id == EPUB.COVER_ID) PROPERTIES_COVER_IMAGE else "")
    renderPage(title, "$id-page", data) {
        tag("div") {
            attr["class"] = "main cover"
            tag("img") {
                attr["src"] = "../$opsPath"
                attr["alt"] = alt
            }
        }
    }
}

private fun writeResource(path: String, id: String, data: Local, properties: String): String {
    return writeFlob(flobOf(path, Local::class.java.classLoader), id, data, properties)
}

private fun writeFlob(flob: Flob, id: String, data: Local, properties: String): String {
    val mimeType = flob.mimeType
    val dir = when {
        mimeType == "text/css" -> data.styleDir
        mimeType.startsWith("image/") -> data.imageDir
        mimeType.startsWith("text/") -> data.textDir
        else -> data.extraDir
    }
    val suffix = mimeType.split('/').last().let {
        when (it) {
            "jpeg" -> "jpg"
            else -> it
        }
    }
    val opsPath = "$dir/$id.$suffix"
    data.writer.writeFlob("${data.opsDir}/$opsPath", flob)
    data.pkg.manifest.addItem(Resource(id, opsPath, mimeType, properties = properties))
    return opsPath
}
