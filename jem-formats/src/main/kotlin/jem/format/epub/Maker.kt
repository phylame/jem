package jem.format.epub

import jclp.LOOSE_DATE_FORMAT
import jclp.flob.Flob
import jclp.setting.Settings
import jclp.setting.getString
import jclp.text.Text
import jclp.vdm.VDMWriter
import jclp.vdm.useStream
import jclp.vdm.write
import jem.*
import jem.epm.VDMMaker
import jem.format.util.XmlRender
import jem.format.util.fail
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

private const val NCX_PATH = "toc.ncx"
private const val NCX_ID = "ncx"
private const val BOOK_ID_NAME = "book-id"
private const val OPF_XML_NS = "http://www.idpf.org/2007/opf"
private val OPTIONAL_ATTRIBUTES = arrayOf("source", "relation", "format")

internal object EpubMaker : VDMMaker {
    override fun make(book: Book, output: VDMWriter, arguments: Settings?) {
        val data = Local(book, output, arguments)
        val version = data.getConfig("version")
        when (version) {
            null, "2.0" -> writeEPUBv2(data)
            else -> fail("epub.make.unsupportedVersion", version)
        }
    }

    private fun writeEPUBv2(data: Local) {
        val book = data.book
        val writer = data.writer
        writer.useStream(MIME_PATH) { it.write(MIME_EPUB.toByteArray()) }

        val opfData = OPFData()

        var idType = "UUID"
        opfData.bookId = data.getConfig("uuid") ?: (book["uuid"] ?: book[ISBN]?.apply {
            idType = "ISBN"
        })?.toString() ?: UUID.randomUUID().toString()

        val lang = data.settings?.get("maker.xml.lang", Locale::class.java) ?: book.language ?: Locale.getDefault()

        writer.useStream(opsPathOf(NCX_PATH)) {
            renderNCX2005(data.render, book, lang.toLanguageTag(), opfData).writeTo(it)
        }
//        result.items += Item(NCX_ID, NCX_PATH, MIME_NCX)

        writer.useStream(opsPathOf("content.opf")) {
            data.render.output(it)
//            renderOPFv2(data.render, bookId, NCX_ID, emptyList(), result.items, result.spines, result.guides)
        }
    }

    private fun writeOPFv2(data: Local, resources: List<Item>, spines: List<Spine>, guides: List<Guide>) {
        data.writer.useStream(data.opsPathOf("content.opf")) {
            with(data.render) {
                output(it)
                beginXml()
                beginTag("package")
                        .attribute("version", "2.0")
                        .attribute("unique-identifier", BOOK_ID_NAME)
                        .xmlns(OPF_XML_NS)
                writeMetadata(data)

                beginTag("manifest")
                for ((id, href, type) in resources) {
                    beginTag("item")
                    attribute("id", id)
                    attribute("href", href)
                    attribute("media-type", type)
                    endTag()
                }
                endTag()

                beginTag("spine").attribute("toc", NCX_ID)
                for ((idref, linear, properties) in spines) {
                    beginTag("itemref")
                    attribute("idref", idref)
                    if (!linear) {
                        attribute("linear", "no");
                    }
                    if (properties.isNotEmpty()) {
                        attribute("properties", properties)
                    }
                    endTag()
                }
                endTag()

                beginTag("guide")
                for (guide in guides) {
                    beginTag("reference")
                    attribute("href", guide.href)
                    attribute("type", guide.type)
                    attribute("title", guide.title)
                    endTag()
                }
                endTag()

                endTag()
                endXml()
            }
        }
    }

    private fun writeMetadata(data: Local) {
        with(data.render) {
            beginTag("metadata")
            attribute("xmlns:dc", "http://purl.org/dc/elements/1.1/")
            attribute("xmlns:opf", OPF_XML_NS)
            writeDcmi(data)
            beginTag("meta")
                    .attribute("name", "cover")
                    .attribute("content", "book-cover")
                    .endTag()
            endTag()
        }
    }

    private fun writeDcmi(data: Local) = with(data.render) {
        beginTag("dc:identifier")
                .attribute("id", BOOK_ID_NAME)
                .attribute("opf:scheme", data.uuidType)
                .text(data.uuid).endTag()
        val book = data.book
        writeDcmiItem(data, TITLE)
        writeDcmiItem(data, AUTHOR, "creator", "aut")
        writeDcmiItem(data, GENRE, "type")
        writeDcmiItem(data, KEYWORDS, "subject")
        writeDcmiItem(data, INTRO, "description")
        writeDcmiItem(data, PUBLISHER)
        (book[PUBDATE] as? LocalDate)?.let {
            beginTag("dc:date")
            attribute("opf:event", "creation")
            text(it.format(data.dateFormat))
            endTag()
        }
        (book.date ?: LocalDate.now()).let {
            beginTag("dc:date")
            attribute("opf:event", "modification")
            text(it.format(data.dateFormat))
            endTag()
        }
        writeDcmiItem(data, LANGUAGE)
        writeDcmiItem(data, RIGHTS)
        writeDcmiItem(data, VENDOR, "contributor", "bkp")
        for (name in OPTIONAL_ATTRIBUTES) {
            writeDcmiItem(data, name)
        }
    }


    private fun writeDcmiItem(data: Local, name: String, dcName: String? = null, role: String? = null) {
        with(data.render) {
            val text = data.book[name]?.toString()?.takeIf { it.isNotEmpty() } ?: return
            beginTag("dc:${dcName ?: name}")
            if (role != null) {
                attribute("opf:role", role)
            }
            text(text)
            endTag()
        }
    }
}

private data class Local(val book: Book, val writer: VDMWriter, val settings: Settings?) {
    val render = XmlRender(settings)

    var uuidType: String = "uuid"

    val uuid: String = getConfig("uuid") ?: (book["uuid"] ?: book["isbn"]?.apply {
        uuidType = "isbn"
    })?.toString() ?: UUID.randomUUID().toString()

    val dateFormat
        inline get() = DateTimeFormatter.ofPattern(settings?.getString("dateFormat") ?: LOOSE_DATE_FORMAT)

    fun getConfig(key: String) = settings?.getString("maker.epub.$key")

    fun opsPathOf(name: String) = "OEBPS/$name"

    fun writeToOps(flob: Flob, name: String) {
        writer.write(opsPathOf(name), flob)
    }

    fun writeToOps(text: Text, name: String, charset: Charset) {
        writer.write(opsPathOf(name), text, charset)
    }
}
