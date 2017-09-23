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

package jem.format.epub

import jclp.io.getMime
import jclp.setting.Settings
import jclp.vdm.VDMWriter
import jclp.vdm.useStream
import jem.*
import jem.format.util.M
import jem.format.util.XmlRender
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.util.*
import kotlin.collections.LinkedHashMap

private const val OPF_XMLNS = "http://www.idpf.org/2007/opf"
private const val DC_XMLNS = "http://purl.org/dc/elements/1.1/"
private const val OPF_META_PREFIX = "jem-opf-meta-"

data class Meta(val name: String, val content: String, val property: String = "")

data class Item(val id: String, val href: String, val type: String)

data class Spine(val idref: String, val linear: Boolean = true, val properties: String = "")

data class Guide(val href: String, val type: String, val title: String)

internal class OPFBuilder(book: Book, writer: VDMWriter, settings: Settings?) : BuilderBase(book, writer, settings) {
    var tocId = ""

    val root: Path = Paths.get(getConfig("opsDir") ?: "OEBPS")

    private val items = LinkedHashMap<String, Item>()

    private val guides = LinkedList<Guide>()

    private val spines = LinkedList<Spine>()

    inline fun newItem(id: String, name: String, mime: String? = null, init: (Path) -> Unit): Pair<Item, Path> {
        val path = root.resolve(name)
        return items.getOrPut(id) {
            init(path)
            Item(id, root.relativize(path).vdmPath, mime ?: getMime(name))
        } to path
    }

    fun newSpine(id: String, linear: Boolean = true, properties: String = "") {
        spines += Spine(id, linear, properties)
    }

    fun newGuide(href: String, type: String, titleId: String) {
        guides += Guide(href, type, M.tr(titleId))
    }

    fun make(): Path {
        val path = root.resolve("content.opf")
        writer.useStream(path.vdmPath) {
            XmlRender(settings).apply {
                output(it)
                renderOPF()
            }
        }
        return path
    }

    private fun XmlRender.renderOPF() {
        beginXml()
        beginTag("package")
        attribute("version", "2.0")
        attribute("unique-identifier", BOOK_ID)
        xmlns(OPF_XMLNS)

        renderMetadata()

        beginTag("manifest")
        for (item in items.values) {
            beginTag("item")
            attribute("id", item.id)
            attribute("href", item.href)
            attribute("media-type", item.type)
            endTag()
        }
        endTag()

        beginTag("spine")
        attribute("toc", tocId)
        for (spine in spines) {
            beginTag("itemref")
            attribute("idref", spine.idref)
            if (!spine.linear) {
                attribute("linear", "no")
            }
            if (spine.properties.isNotEmpty()) {
                attribute("properties", spine.properties)
            }
            endTag()
        }
        endTag()

        beginTag("guide")
        for (guide in guides) {
            beginTag("reference")
            attribute("type", guide.type)
            attribute("title", guide.title)
            attribute("href", guide.href)
            endTag()
        }
        endTag()

        endTag()
        endXml()
    }

    private fun XmlRender.renderMetadata() {
        beginTag("metadata")
        attribute("xmlns:dc", DC_XMLNS)
        attribute("xmlns:opf", OPF_XMLNS)

        newDublin("identifier", uuid, id = BOOK_ID, scheme = if ("isbn" in uuid) "ISBN" else "UUID")
        newDublin("title")
        for (author in book.author.split(Attributes.VALUE_SEPARATOR)) {
            newDublin("creator", author, role = "aut")
        }
        newDublin("type", book.genre)
        book[KEYWORDS]?.toString()?.split(Attributes.VALUE_SEPARATOR)?.forEach {
            newDublin("subject", it)
        }
        newDublin("description", book.intro?.toString() ?: "")
        newDublin("publisher")
        (book[PUBDATE] as? LocalDate)?.let {
            newDublin("date", it.toString(), event = "publication")
        }
        (book.date ?: LocalDate.now())?.let {
            newDublin("date", it.toString(), event = "modification")
        }
        newDublin("language", lang)
        newDublin("rights")
        newDublin("contributor", book.vendor, role = "bkp")
        newDublin("publisher")

        newMeta("jem:version", Build.VERSION)
        newMeta("jem:vendor", Build.VENDOR)

        book.extensions.filter { it.first.startsWith(OPF_META_PREFIX) }.forEach { (name, value) ->
            newMeta(name.substring(OPF_META_PREFIX.length), value.toString())
        }

        for (meta in meta.values) {
            beginTag("meta")
            if (meta.property.isNotEmpty()) {
                attribute("property", meta.property)
                text(meta.content)
            } else {
                attribute("name", meta.name)
                attribute("content", meta.content)
            }
            endTag()
        }

        endTag()
    }

    private fun XmlRender.newDublin(name: String) {
        newDublin(name, book[name]?.toString() ?: return)
    }

    private fun XmlRender.newDublin(name: String, value: String, id: String = "", role: String = "", event: String = "", scheme: String = "") {
        if (value.isEmpty()) {
            return
        }
        beginTag("dc:$name")
        if (id.isNotEmpty()) {
            attribute("id", id)
        }
        if (role.isNotEmpty()) {
            attribute("opf:role", role)
        }
        if (event.isNotEmpty()) {
            attribute("opf:event", event)
        }
        if (scheme.isNotEmpty()) {
            attribute("opf:scheme", scheme)
        }
        text(value)
        endTag()
    }
}
