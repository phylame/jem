package jem.format.epub

import jclp.flob.Flob
import jclp.io.extensionName
import jclp.io.getProperties
import jclp.log.Log
import jclp.setting.Settings
import jclp.setting.getString
import jclp.vdm.VDMWriter
import jclp.vdm.useStream
import jem.*
import jem.epm.VDMMaker
import jem.format.util.M
import jem.format.util.fail
import org.apache.velocity.Template
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import java.nio.file.Path
import java.util.*
import kotlin.collections.LinkedHashMap

internal object EpubMaker : VDMMaker {
    override fun make(book: Book, output: VDMWriter, arguments: Settings?) {
        val version = arguments?.getString("epub.make.version") ?: ""
        when (version) {
            "", "2.0" -> writeEPUBv2(book, output, arguments)
            else -> fail("epub.make.unsupportedVersion", version)
        }
    }

    private fun writeEPUBv2(book: Book, writer: VDMWriter, settings: Settings?) {
        writer.useStream(MIME_PATH) { it.write(MIME_EPUB.toByteArray()) }

        val opf = OPFBuilder(book, writer, settings)
        val ncx = NCXBuilder(book, writer, settings, opf)
        val toc = TOCBuilder(book, writer, settings, ncx, opf)

        toc.make()
        ncx.make()
        val path = opf.make().vdmPath

        writeContainer(writer, settings, mapOf(path to MIME_OPF))
    }
}

internal open class BuilderBase(val book: Book, val writer: VDMWriter, val settings: Settings?) {
    val meta = LinkedHashMap<String, Meta>()

    val lang: String = (book.language ?: Locale.getDefault()).toLanguageTag()

    val uuid = book["uuid"]?.let { "urn:uuid:$it" } ?: book[ISBN]?.let { "urn:isbn:$it" } ?: "urn:uuid:${UUID.randomUUID()}"

    fun newMeta(name: String, content: String, property: String = "") {
        if (name !in meta) {
            meta[name] = Meta(name, content, property)
        } else {
            Log.w(javaClass.simpleName) { "meta of name '$name' already exists" }
        }
    }

    fun getConfig(name: String) = settings?.getString("epub.make.$name")
}

internal class TOCBuilder(
        book: Book,
        writer: VDMWriter,
        settings: Settings?,
        private val ncx: NCXBuilder,
        private val opf: OPFBuilder
) : BuilderBase(book, writer, settings) {
    private val styleDir = getConfig("styleDir") ?: "styles/"
    private val imageDir = getConfig("imageDir") ?: "images/"
    private val extraDir = getConfig("extraDir") ?: "extras/"
    private val textDir = getConfig("textDir") ?: "text/"

    private val textRoot: Path = opf.root.resolve(textDir)

    private lateinit var cssHref: String

    private fun newContext() = VelocityContext().apply {
        put("M", M)
        put("book", book)
        put("lang", lang)
        put("cssHref", cssHref)
    }

    fun make() {
        writeFlob(Flob.of("!jem/format/epub/pat.png"), "pat")
        cssHref = relativeToText(writeFlob(Flob.of("!jem/format/epub/style.css"), "style").second)
        book.cover?.let {
            val context = newContext()
            context.put("coverHref", relativeToText(writeFlob(it, COVER_ID).second))
            opf.newMeta("cover", COVER_ID)
            val (item, _) = renderPage("cover-page", context, "cover")
            opf.newSpine(item.id, properties = "duokan-page-fullscreen")
            opf.newGuide(item.href, "cover", "epub.make.coverGuide")
        }
        book.intro?.let {
            val context = newContext()
            context.put("intro", it)
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
        var item: Item? = null
        if (chapter.isSection) {
            if (hasCover || chapter.intro != null) {
                item = renderPage(id, context, "section").first
            }
        } else {
            if (hasCover || chapter.intro != null) {
                val pageId = "$id-cover-page"
                renderPage(pageId, context, "section").first
                opf.newSpine(pageId)
            }
            item = renderPage(id, context, "chapter").first
        }

        if (item != null) {
            opf.newSpine(id)
            if (ncx.count == 1) {
                opf.newGuide(item.href, "text", "epub.make.textTitle")
            }
        }
        ncx.newNav(id, chapter.title, item?.href ?: "")
        chapter.forEachIndexed { i, stub ->
            renderToc(stub, "$suffix-${i + 1}")
        }
        ncx.endNav()
    }

    private fun renderPage(id: String, context: VelocityContext, name: String? = null): Pair<Item, Path> {
        return opf.newItem(id, "$textDir/$id.xhtml", MIME_XHTML) {
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
        val mime = flob.mime
        return opf.newItem(id, "${classifyDir(mime)}$id${extensionName(flob.name)}", mime) {
            writer.useStream(it.vdmPath) {
                flob.writeTo(it)
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
