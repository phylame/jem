package jem.format.epub.v2

import jclp.depth
import jclp.text.ifNotEmpty
import jclp.xml.attribute
import jclp.xml.endTag
import jclp.xml.lang
import jclp.xml.startTag
import jem.Attributes
import jem.author
import jem.format.epub.DataHolder
import jem.format.epub.EPUB
import jem.format.epub.html.Nav
import jem.format.util.xmlSerializer
import jem.title
import org.xmlpull.v1.XmlSerializer

internal fun makeNcx2005(data: DataHolder) {
    val name = "toc.ncx"
    data.writer.xmlSerializer("${data.opsDir}/$name", data.settings) {
        startTag("ncx")
        attribute("version", "2005-1")
        attribute("xmlns", EPUB.XMLNS_NCX)
        lang(data.lang)

        writeHead(data)

        startTag("docTitle")
        startTag("text")
        text(data.book.title)
        endTag()
        endTag()

        for (author in data.book.author.split(Attributes.VALUE_SEPARATOR)) {
            startTag("docAuthor")
            startTag("text")
            text(author)
            endTag()
            endTag()
        }

        val textDir = data.textDir
        startTag("navMap")
        for (nav in data.nav.items) {
            writeNav(nav, textDir)
        }
        endTag()

        endTag()
    }
    data.pkg.manifest.addResource("ncx", name, EPUB.MIME_NCX)
}

private fun XmlSerializer.writeHead(data: DataHolder) {
    startTag("head")
    writeMeta("dtb:uid", data.bookId)
    writeMeta("dtb:depth", data.book.depth.toString())
    writeMeta("dtb:totalPageCount", "0")
    writeMeta("dtb:maxPageNumber", "0")
    endTag()
}

private fun XmlSerializer.writeMeta(name: String, content: String) {
    startTag("meta")
    attribute("name", name)
    attribute("content", content)
    endTag()
}

private fun XmlSerializer.writeNav(nav: Nav, textDir: String) {
    startTag("navPoint")
    attribute("class", if (nav.items.isEmpty()) "chapter" else "section")
    attribute("id", nav.id)

    startTag("navLabel")
    startTag("text")
    text(nav.title)
    endTag()
    endTag()

    nav.usableHref.ifNotEmpty {
        startTag("content")
        attribute("src", "$textDir/$it")
        endTag()
    }

    nav.items.forEach { writeNav(it, textDir) }

    endTag()
}
