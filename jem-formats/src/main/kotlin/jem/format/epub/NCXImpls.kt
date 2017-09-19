package jem.format.epub

import jclp.depth
import jem.Book
import jem.author
import jem.format.util.XmlRender
import jem.title
import java.io.ByteArrayOutputStream

private const val DTD_ID = "-//NISO//DTD ncx 2005-1//EN"
private const val DTD_URI = "http://www.daisy.org/z3986/2005/ncx-2005-1.dtd"

private const val VERSION = "2005-1"
private const val NAMESPACE = "http://www.daisy.org/z3986/2005/ncx/"

internal fun renderNCX2005(render: XmlRender, book: Book, lang: String, data: OPFData) = with(render) {
    val stream = ByteArrayOutputStream()
    render.output(stream)

    beginXml()
    docdecl("ncx", DTD_ID, DTD_URI)
    beginTag("ncx")
    attribute("version", VERSION)
    attribute("xml:lang", lang)
    xmlns(NAMESPACE)

    writeHead(this, data.bookId, 0, 0, book.depth)

    beginTag("docTitle")
    beginTag("text").text(book.title).endTag()
    endTag()

    beginTag("docAuthor")
    beginTag("text").text(book.author).endTag()
    endTag()

    beginTag("navMap")
    endTag()

    endTag()
    endXml()

    stream
}

private fun writeHead(render: XmlRender, uuid: String, pages: Int, number: Int, depth: Int) = with(render) {
    beginTag("head")
    writeMeta(render, "dtb:uid", uuid)
    writeMeta(render, "dtb:totalPageCount", pages.toString())
    writeMeta(render, "dtb:maxPageNumber", number.toString())
    writeMeta(render, "dtb:depth", depth.toString())
    endTag()
}

private fun writeMeta(render: XmlRender, name: String, content: String) = with(render) {
    beginTag("meta")
    attribute("name", name)
    attribute("content", content)
    endTag()
}
