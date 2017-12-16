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
import jclp.vdm.writeFlob
import jclp.xml.Tag
import jem.*
import jem.format.epub.EPUB
import jem.format.epub.writeContainer
import jem.format.util.*
import java.io.InterruptedIOException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.util.*
import jem.format.util.M as T

private class Local(val book: Book, val writer: VdmWriter, val settings: Settings?) {
    val encoding = settings.xmlEncoding

    val lineSeparator = settings.xmlSeparator

    val opsDir = getConfig("opsDir") ?: "EPUB"

    val styleDir = getConfig("styleDir") ?: "Styles"

    val imageDir = getConfig("imageDir") ?: "Images"

    val extraDir = getConfig("extraDir") ?: "Extras"

    val textDir = getConfig("textDir") ?: "Text"

    val useDuokanCover = getConfig("useDuokanCover") ?: true

    val lang: String =
            getConfig("language") ?: book.language?.toLanguageTag() ?: Locale.getDefault().toLanguageTag()

    var cssHref: String = ""

    var maskHref: String = ""

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

internal fun makeImpl30(book: Book, writer: VdmWriter, settings: Settings?) {
    val data = Local(book, writer, settings)

    writeMetadata(data)

    writeContents(data)

    val opfPath = "${data.opsDir}/package.opf"
    writer.xmlSerializer(opfPath, settings) {
        data.pkg.renderTo(this)
    }

    writeContainer(writer, settings, mapOf(opfPath to EPUB.MIME_OPF))
}

private fun writeMetadata(data: Local) {
    val pkg = data.pkg
    val md = pkg.metadata
    val book = data.book

    pkg.language = data.lang

    book[ISBN]?.toString()?.ifNotEmpty {
        md.addISBN("isbn", "urn:isbn:$it")
        pkg.uniqueIdentifier = "isbn"
    }
    book["uuid"]?.toString()?.ifNotEmpty {
        md.addISBN("uuid", "urn:uuid:$it")
        if (pkg.uniqueIdentifier.isEmpty()) {
            pkg.uniqueIdentifier = "uuid"
        }
    }
    if (pkg.uniqueIdentifier.isEmpty()) {
        md.addUUID("uuid", "urn:uuid:${UUID.randomUUID()}")
        pkg.uniqueIdentifier = "uuid"
    }

    md.addModifiedTime(OffsetDateTime.now().toInstant())

    md.addTitle(book.title)
    md.addDCME("language", data.lang)

    book.author.split(Attributes.VALUE_SEPARATOR).forEach {
        if (it.isNotEmpty()) md.addAuthor(it)
    }
    book.vendor.split(Attributes.VALUE_SEPARATOR).forEach {
        if (it.isNotEmpty()) md.addVendor(it)
    }
    book.intro?.ifNotEmpty {
        md.addDCME("description", it.joinToString(separator = data.lineSeparator, transform = String::trim))
    }
    book[KEYWORDS]?.toString()?.split(Attributes.VALUE_SEPARATOR)?.forEach {
        if (it.isNotEmpty()) md.addDCME("subject", it)
    }
    book.publisher.ifNotEmpty {
        md.addDCME("publisher", it)
    }
    (book[PUBDATE] as? LocalDate)?.let {
        md.addPubdate(it.atTime(OffsetTime.MIN).toInstant())
    }
    book.rights.ifNotEmpty {
        md.addDCME("rights", it)
    }
}

private fun writeContents(data: Local) {
    val book = data.book

    val cssPath = writeResource(data.getConfig("cssPath") ?: "!jem/format/epub/v3/main.css", "style", data, "")
    data.cssHref = "../$cssPath"

    val maskPath = writeResource(data.getConfig("maskPath") ?: "!jem/format/epub/v3/mask.png", "mask", data, "")
    data.maskHref = "../$maskPath"

    book.cover?.let {
        renderCover(it, data)
    }

    book.intro?.ifNotEmpty {
        renderIntro(it, data)
    }

    // nav
    val navTitle = T.tr("epub.make.navTitle")
    data.pkg.spine.addReference("nav", false)
    data.nav.newNav(navTitle, "nav.xhtml")

    renderText(book, "", data)

    renderPage(navTitle, "nav", false, data) {
        tag("div") {
            attr["class"] = "main toc"
            tag("h1", navTitle)
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
    renderImage(cover, "cover", T.tr("epub.make.coverTitle"), data.book.title, true, data)
}

private fun renderIntro(intro: Text, data: Local) {
    val title = T.tr("epub.make.introTitle")
    renderPage(title, "intro", true, data) {
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

    if (intro?.isEmpty() != false) { // with cover, no intro
        renderImage(cover!!, "$id-cover", section.title, section.title, true, data)
        return
    }

    val coverHref = cover?.let {
        writeFlob(it, "$id-cover", data, "")
    }

    renderPage(title, id, true, data) {
        if (coverHref != null) {
            attr["style"] = "background-image: url(${"../$coverHref"});background-size: cover;"
            tag("div") {
                intro.ifNotEmpty {
                    attr["style"] = "background: url(${data.maskHref}) repeat;"
                    attr["class"] = "main section"
                    intro.forEach { tag("p", it.trim()) }
                }
            }
        } else {
            intro.ifNotEmpty {
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
        renderImage(cover, "$id-cover", title, title, false, data)
    }

    if (intro?.isEmpty() != false && text?.isEmpty() != false) {
        data.nav.newNav(title, "")
        return
    }

    renderPage(title, id, true, data) {
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

private fun renderImage(image: Flob, id: String, title: String, altText: String, addToNav: Boolean, data: Local) {
    val coverHref = writeFlob(image, id, data, if (id == "cover") EPUB.MANIFEST_COVER_IMAGE else "")
    renderPage(title, "$id-page", addToNav, data) {
        tag("div") {
            attr["class"] = "main cover"
            tag("img") {
                attr["src"] = "../$coverHref"
                attr["alt"] = altText
            }
        }
    }
}

private inline fun renderPage(title: String, id: String, addToNav: Boolean, data: Local, block: Tag.() -> Unit) {
    with(data) {
        val opsPath = "$textDir/$id.xhtml"
        writer.xmlDsl("$opsDir/$opsPath", settings) {
            doctype("html")
            tag("html") {
                attr["xmlns"] = EPUB.XMLNS_XHTML
                attr["xmlns:epub"] = EPUB.XMLNS_OPS
                attr["lang"] = lang
                tag("head") {
                    tag("meta") {
                        attr["charset"] = encoding
                    }
                    tag("title", title)
                    tag("link") {
                        attr["rel"] = "stylesheet"
                        attr["type"] = EPUB.MIME_CSS
                        attr["href"] = cssHref
                    }
                }
                tag("body", block)
            }
        }
        if (addToNav) nav.newNav(title, "$id.xhtml")
        pkg.manifest.addResource(id, opsPath, EPUB.MIME_XHTML, if (id == "nav") EPUB.MANIFEST_NAVIGATION else "")
        if (id != "nav") { // nav is already added
            pkg.spine.addReference(id, properties = if (useDuokanCover && "cover" in id) EPUB.SPINE_DUOKAN_FULLSCREEN else "")
        }
    }
}

private fun writeResource(path: String, id: String, data: Local, properties: String): String {
    return writeFlob(flobOf(path, Local::class.java.classLoader), id, data, properties)
}

private fun writeFlob(flob: Flob, id: String, data: Local, properties: String): String {
    with(data) {
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
        pkg.manifest.addResource(id, opsPath, mime, properties)
        return opsPath
    }
}
