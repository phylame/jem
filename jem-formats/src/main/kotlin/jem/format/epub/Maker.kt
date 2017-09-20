package jem.format.epub

import jclp.LOOSE_DATE_FORMAT
import jclp.setting.Settings
import jclp.setting.getString
import jclp.vdm.VDMWriter
import jclp.vdm.useStream
import jem.*
import jem.epm.VDMMaker
import jem.format.util.XmlRender
import jem.format.util.fail
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.LinkedHashMap

private const val NCX_PATH = "toc.ncx"
private const val OPF_PATH = "content.opf"
private const val OPF_META_PREFIX = "jem-opf-meta-"
private val OPTIONAL_ATTRIBUTES = arrayOf("source", "relation", "format")

internal object EpubMaker : VDMMaker {
    override fun make(book: Book, output: VDMWriter, arguments: Settings?) {
        val param = EpubParam(book, output, arguments)
        val version = param.getConfig("version") ?: ""
        when (version) {
            "", "2.0" -> writeEPUBv2(param)
            else -> fail("epub.make.unsupportedVersion", version)
        }
    }

    private fun writeEPUBv2(param: EpubParam) {
        val book = param.book
        val writer = param.writer
        val render = param.render
        writer.useStream(MIME_PATH) { it.write(MIME_EPUB.toByteArray()) }

        val data = OpfData()
        data.lang = (book.language ?: Locale.getDefault()).toLanguageTag()
        data.bookId = param.getConfig("bookId") ?: book["uuid"]?.let {
            "urn:uuid:$it"
        } ?: book[ISBN]?.let {
            "urn:isbn:$it"
        } ?: "urn:uuid:${UUID.randomUUID()}"

        val buffer = ByteArrayOutputStream()
        render.output(buffer)
        val navListener = renderNCX("2005", param, data)
        writer.useStream(opsPathOf(NCX_PATH)) { buffer.writeTo(it) }
        data.resources[param.ncxId] = Item(param.ncxId, NCX_PATH, MIME_NCX)

        param.tocRender.addNavListener(navListener)
        param.tocRender.render(book, writer, param.settings, data)

        val opfPath = opsPathOf(OPF_PATH)
        writer.useStream(opfPath) {
            render.output(it)
            makeMetadata(param, data)
            renderOPF("2.0", param, data)
        }

        writer.useStream(CONTAINER_PATH) {
            render.output(it)
            renderContainer(render, mapOf(opfPath to MIME_OPF))
        }
    }

    private fun makeMetadata(param: EpubParam, data: OpfData) {
        val book = param.book
        val meta = LinkedHashMap<String, Any>()
        data.metadata.let {
            it += Dublin("identifier", data.bookId, id = BOOK_ID, schema = if ("isbn" in data.bookId) "ISBN" else "UUID")
            it += Dublin("title", book.title)
            book.author.split(";").forEach { author ->
                it += Dublin("creator", author, role = "aut")
            }
            it += Dublin("type", book.genre)
            it += Dublin("subject", book[KEYWORDS]?.toString() ?: "")
            it += Dublin("description", book.intro?.toString() ?: "")
            it += Dublin("publisher", book.publisher)
            (book[PUBDATE] as? LocalDate)?.apply {
                it += Dublin("date", format(param.dateFormat), event = "creation")
            }
            (book.date ?: LocalDate.now()).apply {
                it += Dublin("date", format(param.dateFormat), event = "modification")
            }
            it += Dublin("language", data.lang)
            it += Dublin("rights", book.rights)
            it += Dublin("contributor", book.vendor, role = "bkp")
            it += Dublin("publisher", book.publisher)
        }
        for (name in OPTIONAL_ATTRIBUTES) {
            meta[name] = Meta(name, book[name]?.toString() ?: continue)
        }
        for ((first, second) in book.extensions) {
            if (first.startsWith(OPF_META_PREFIX)) {
                val key = first.substring(OPF_META_PREFIX.length)
                meta[key] = Meta(key, second.toString())
            }
        }
        (param.settings?.get("maker.epub.opfMeta") as? Map<*, *>)?.let {
            for ((key, value) in it) {
                meta[key.toString()] = Meta(key.toString(), value.toString())
            }
        }
        data.metadata.addAll(meta.values)
    }
}

internal data class EpubParam(val book: Book, val writer: VDMWriter, val settings: Settings?) {
    val ncxId = "ncx"

    val render = XmlRender(settings)

    val tocRender = DefaultTOCRender

    val dateFormat: DateTimeFormatter
        inline get() = DateTimeFormatter.ofPattern(settings?.getString("dateFormat") ?: LOOSE_DATE_FORMAT)

    fun getConfig(key: String) = settings?.getString("maker.epub.$key")
}
