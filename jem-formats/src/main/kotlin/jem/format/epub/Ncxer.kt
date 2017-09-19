package jem.format.epub

import jclp.depth
import jclp.setting.Settings
import jem.Book
import jem.author
import jem.format.util.XmlRender
import jem.language
import jem.title
import java.io.ByteArrayOutputStream
import java.util.*


private const val DTD_ID = "-//NISO//DTD ncx 2005-1//EN"
private const val DTD_URI = "http://www.daisy.org/z3986/2005/ncx-2005-1.dtd"

private const val VERSION = "2005-1"
private const val NAMESPACE = "http://www.daisy.org/z3986/2005/ncx/"

internal class NcxResult(book: Book, settings: Settings?) {
    val guides = LinkedList<Guide>()

    val spines = LinkedList<Spine>()

    val resources = LinkedList<Item>()

    val buffer = ByteArrayOutputStream()

    val xmlLang: Locale = settings?.get("maker.xml.lang", Locale::class.java) ?: book.language ?: Locale.getDefault()
}

internal fun renderNcx2005(data: EpubLocal): NcxResult {
    val book = data.book
    val render = data.render
    val result = NcxResult(book, data.settings)
    render.output(result.buffer)
            .beginXml()
            .docdecl("ncx", DTD_ID, DTD_URI)
            .beginTag("ncx")
            .attribute("version", VERSION)
            .attribute("xml:lang", result.xmlLang.toLanguageTag())
            .xmlns(NAMESPACE)
    writeHead(render, book.depth, data.uuid, 0, 0)
    render.beginTag("docTitle")
            .beginTag("text").text(book.title).endTag()
            .endTag()
    book.author.takeIf(String::isNotEmpty)?.let {
        render.beginTag("docAuthor")
                .beginTag("text").text(it).endTag()
                .endTag()
    }
    render.beginTag("navMap")
            .endTag()
    render.endTag().endXml()
    return result
}

private fun writeHead(render: XmlRender, depth: Int, uuid: String, totalPageCount: Int, maxPageNumber: Int) {
    render.beginTag("head")
    writeMeta(render, "dtb:uid", uuid)
    writeMeta(render, "dtb:totalPageCount", totalPageCount.toString())
    writeMeta(render, "dtb:maxPageNumber", maxPageNumber.toString())
    writeMeta(render, "dtb:depth", depth.toString())
    render.endTag()
}

private fun writeMeta(render: XmlRender, name: String, value: String) {
    render.beginTag("meta")
            .attribute("name", name)
            .attribute("content", value)
            .endTag()
}