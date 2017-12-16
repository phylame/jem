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

import jclp.setting.Settings
import jclp.text.ifNotEmpty
import jclp.vdm.VdmWriter
import jclp.xml.Tag
import jem.*
import jem.format.epub.DataHolder
import jem.format.epub.EPUB
import jem.format.epub.html.*
import jem.format.util.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.util.*
import jem.format.util.M as T

internal fun makeImpl301(book: Book, writer: VdmWriter, settings: Settings?): DataHolder {
    val data = DataHolder("3.0", book, writer, settings)
    writeMetadata(data)
    writeContents(data)
    return data
}

private fun writeMetadata(data: DataHolder) {
    val pkg = data.pkg
    val md = pkg.metadata
    val book = data.book

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
        md.addDCME("description", it.joinToString(separator = data.separator, transform = String::trim))
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

private fun writeContents(data: DataHolder) {
    val book = data.book
    data.epubVersion = 3

    data.cssHref = "../${data.writeResource(data.cssPath, "style").href}"
    data.maskHref = "../${data.writeResource(data.maskPath, "mask").href}"

    book.cover?.let {
        renderCover(it, "cover", T.tr("epub.make.coverTitle"), book.title, data)
    }

    book.intro?.ifNotEmpty {
        renderIntro(it, T.tr("epub.make.introTitle"), data)
    }

    // nav
    val navTitle = T.tr("epub.make.navTitle")
    data.pkg.spine.addRef("nav", false)
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
    }.also {
        it.attr["properties"] = EPUB.MANIFEST_NAVIGATION
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
