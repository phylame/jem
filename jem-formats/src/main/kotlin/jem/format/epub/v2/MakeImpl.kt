package jem.format.epub.v2

import jclp.setting.Settings
import jclp.text.ifNotEmpty
import jclp.vdm.VdmWriter
import jem.*
import jem.format.epub.DataHolder
import jem.format.epub.EPUB
import jem.format.epub.html.renderCover
import jem.format.epub.html.renderIntro
import jem.format.epub.html.renderText
import jem.format.util.M
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.util.*

private class Local(book: Book, writer: VdmWriter, settings: Settings?) : DataHolder("2.0", book, writer, settings) {
    var coverHref = ""
}

internal fun makeImpl201(book: Book, writer: VdmWriter, settings: Settings?): DataHolder {
    val data = Local(book, writer, settings)
    writeMetadata(data)
    writeContents(data)
    makeNcx2005(data)
    return data
}

private fun writeMetadata(data: Local) {
    val pkg = data.pkg
    val md = pkg.metadata
    val book = data.book

    md.attr["xmlns:opf"] = EPUB.XMLNS_OPF

    book[ISBN]?.toString()?.ifNotEmpty {
        data.bookId = "urn:isbn:$it"
        md.addISBN("isbn", data.bookId)
        pkg.uniqueIdentifier = "isbn"
    }
    book["uuid"]?.toString()?.ifNotEmpty {
        val uid = "urn:uuid:$it"
        md.addUUID("uuid", uid)
        if (pkg.uniqueIdentifier.isEmpty()) {
            pkg.uniqueIdentifier = "uuid"
            data.bookId = uid
        }
    }
    if (pkg.uniqueIdentifier.isEmpty()) {
        data.bookId = "urn:uuid:${UUID.randomUUID()}"
        md.addUUID("uuid", data.bookId)
        pkg.uniqueIdentifier = "uuid"
    }

    md.addModifiedTime(OffsetDateTime.now().toInstant())

    md.addDCME("title", book.title)
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
    book[PRICE]?.toString()?.ifNotEmpty {
        md.addMeta("price", it)
    }
    book.cover?.let {
        data.coverHref = data.writeFlob(it, "cover").href
        md.addMeta("cover", "cover")
    }
}

private fun writeContents(data: Local) {
    val book = data.book
    val spine = data.pkg.spine
    data.epubVersion = 2

    data.cssHref = "../${data.writeResource(data.cssPath, "style").href}"
    data.maskHref = "../${data.writeResource(data.maskPath, "mask").href}"

    data.coverHref.ifNotEmpty {
        renderCover(it, "cover-page", M.tr("epub.make.coverTitle"), book.title, data)
    }
    book.intro?.ifNotEmpty {
        renderIntro(it, M.tr("epub.make.introTitle"), data)
    }
    spine.toc = "ncx"
    renderText(book, "", data)
}
