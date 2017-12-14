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
import jclp.io.extName
import jclp.io.flobOf
import jclp.io.getProperties
import jclp.log.Log
import jclp.setting.Settings
import jclp.setting.getString
import jclp.text.TEXT_HTML
import jclp.text.Text
import jclp.text.TextWrapper
import jclp.text.ifNotEmpty
import jclp.vdm.VdmWriter
import jclp.vdm.useStream
import jclp.vdm.writeBytes
import jem.*
import jem.epm.VdmMaker
import jem.format.epub.v3.makeImpl
import jem.format.util.M
import jem.format.util.failMaker
import org.apache.velocity.Template
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import java.nio.file.Path
import java.util.*

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
        val version = arguments?.getString("maker.epub.version") ?: ""
        when (version) {
            "", "2", "2.0", "2.0.1" -> writeEPUBv2(book, output, arguments)
            "3", "3.0" -> writeEPUBv3(book, output, arguments)
            else -> failMaker("epub.make.unsupportedVersion", version)
        }
    }

    private fun writeEPUBv2(book: Book, writer: VdmWriter, settings: Settings?) {
        writer.writeBytes(EPUB.MIME_PATH, EPUB.MIME_EPUB.toByteArray())

        val opf = OPFBuilder(book, writer, settings)
        val ncx = NCXBuilder(book, writer, settings, opf)
        val toc = TOCBuilder(book, writer, settings, ncx, opf)

        toc.make()
        ncx.make()

        writeContainer(writer, settings, mapOf(opf.make().vdmPath to EPUB.MIME_OPF))
    }

    private fun writeEPUBv3(book: Book, writer: VdmWriter, settings: Settings?) {
        writer.writeBytes(EPUB.MIME_PATH, EPUB.MIME_EPUB.toByteArray())
        makeImpl(book, writer, settings)
    }
}

internal open class BuilderBase(val book: Book, val writer: VdmWriter, val settings: Settings?) {
    val meta = linkedMapOf<String, Meta>()

    val lang: String = (book.language ?: Locale.getDefault()).toLanguageTag()

    val uuid = book["uuid"]?.let { "urn:uuid:$it" } ?: book[ISBN]?.let { "urn:isbn:$it" } ?: "urn:uuid:${UUID.randomUUID()}"

    fun newMeta(name: String, content: String, property: String = "") {
        if (name !in meta) {
            meta[name] = Meta(name, content, property)
        } else {
            Log.w(javaClass.simpleName) { "meta of name '$name' already exists" }
        }
    }

    fun getConfig(name: String) = settings?.getString("maker.epub.$name")
}

internal class TOCBuilder(
        book: Book,
        writer: VdmWriter,
        settings: Settings?,
        private val ncx: NCXBuilder,
        private val opf: OPFBuilder
) : BuilderBase(book, writer, settings) {
    private val styleDir = getConfig("styleDir") ?: "Styles/"
    private val imageDir = getConfig("imageDir") ?: "Images/"
    private val extraDir = getConfig("extraDir") ?: "Extras/"
    private val textDir = getConfig("textDir") ?: "Text/"

    private val textRoot: Path = opf.root.resolve(textDir)

    private lateinit var cssHref: String

    private lateinit var maskHref: String

    private fun newContext() = VelocityContext().apply {
        put("M", M)
        put("book", book)
        put("lang", lang)
        put("cssHref", cssHref)
        put("maskHref", maskHref)
    }

    private operator fun VelocityContext.set(name: String, text: Text) {
        put(name, object : TextWrapper(text) {
            var count = 0 // line counter

            override fun iterator() = super.iterator().asSequence().map {
                ++count
                it.trim()
            }.iterator()
        })
    }

    fun make() {
        cssHref = relativeToText(writeFlob(flobOf("!jem/format/epub/style.css"), "style").second)
        maskHref = relativeToText(writeFlob(flobOf("!jem/format/epub/mask.png"), "mask").second)
        book.cover?.let {
            val context = newContext()
            context.put("coverHref", relativeToText(writeFlob(it, EPUB.COVER_ID).second))
            opf.newMeta("cover", EPUB.COVER_ID)
            val (item, _) = renderPage("cover-page", context, "cover")
            opf.newSpine(item.id, properties = EPUB.SPINE_DUOKAN_FULLSCREEN)
            opf.newGuide(item.href, "cover", "epub.make.coverGuide")
        }
        book.intro?.toString()?.ifNotEmpty {
            val context = newContext()
            context["intro"] = book.intro!!
            val (item, _) = renderPage("intro", context)
            opf.newSpine(item.id)
            ncx.newNav(item.id, M.tr("epub.make.introTitle"), item.href)
            ncx.endNav()
        }
        book.forEachIndexed { i, chapter ->
            renderToc(chapter, (i + 1).toString())
        }
    }

    private fun renderToc(chapter: Chapter, suffix: String) {
        val context = newContext()
        context.put("chapter", chapter)
        val id = "chapter-$suffix"

        var hasCover = false
        chapter.cover?.let {
            hasCover = true
            context.put("coverHref", relativeToText(writeFlob(it, "$id-cover").second))
        }

        var hasIntro = false
        chapter.intro?.toString()?.ifNotEmpty {
            hasIntro = true
            context["intro"] = chapter.intro!!
        }

        var item: Item? = null
        if (chapter.isSection) {
            if (hasIntro) {
                item = renderPage(id, context, "section").first
                opf.newSpine(id)
            } else if (hasCover) {
                item = renderPage(id, context, "cover").first
                opf.newSpine(id, properties = EPUB.SPINE_DUOKAN_FULLSCREEN)
            }
        } else {
            if (hasCover) {
                val pageId = "$id-cover-page"
                renderPage(pageId, context, "cover").first
                opf.newSpine(pageId, properties = EPUB.SPINE_DUOKAN_FULLSCREEN)
            }
            chapter.text?.let {
                if (it.type == TEXT_HTML) {
                    item = writeText(it, id, EPUB.MIME_XHTML).first
                    opf.newSpine(id)
                } else {
                    context["text"] = it
                    item = renderPage(id, context, "chapter").first
                    opf.newSpine(id)
                }
            }
        }
        if (item != null && ncx.count == 1) { // first chapter
            opf.newGuide(item!!.href, "text", "epub.make.textTitle")
        }
        ncx.newNav(id, chapter.title, item?.href ?: "")
        chapter.forEachIndexed { i, stub ->
            renderToc(stub, "$suffix-${i + 1}")
        }
        ncx.endNav()
    }

    private fun renderPage(id: String, context: VelocityContext, name: String? = null): Pair<Item, Path> {
        return opf.newItem(id, "$textDir/$id.xhtml", EPUB.MIME_XHTML) {
            writer.useStream(it.vdmPath) {
                it.writer(Charsets.UTF_8).apply {
                    Templates.getTemplate(name ?: id).merge(context, this)
                    flush()
                }
            }
        }
    }

    private fun relativeToText(path: Path) = textRoot.relativize(path).vdmPath

    private fun writeFlob(flob: Flob, id: String): Pair<Item, Path> {
        val mime = flob.mimeType
        return opf.newItem(id, "${classifyDir(mime)}$id.${extName(flob.name)}", mime) {
            writer.useStream(it.vdmPath) {
                flob.writeTo(it)
            }
        }
    }

    private fun writeText(text: Text, id: String, mime: String): Pair<Item, Path> {
        val type = text.type
        return opf.newItem(id, "$textDir/$id.$type", mime) {
            writer.useStream(it.vdmPath) {
                it.writer(Charsets.UTF_8).apply {
                    text.writeTo(this)
                    flush()
                }
            }
        }
    }

    private fun classifyDir(mime: String) = when {
        mime.endsWith("css") -> styleDir
        mime.startsWith("text") -> textDir
        mime.startsWith("image") -> imageDir
        else -> extraDir
    }
}

internal object Templates {
    init {
        Velocity.init(getProperties("!jem/format/epub/velocity.properties"))
    }

    fun getTemplate(name: String): Template = Velocity.getTemplate("jem/format/epub/$name.vm")
}
