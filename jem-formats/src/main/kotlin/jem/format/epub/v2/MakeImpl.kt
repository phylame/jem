package jem.format.epub.v2

import jclp.setting.Settings
import jclp.text.ifNotEmpty
import jclp.vdm.VdmWriter
import jem.*
import jem.format.epub.DataHolder
import jem.format.epub.EPUB
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.util.*

private class Local(book: Book, writer: VdmWriter, settings: Settings?) : DataHolder("2.0", book, writer, settings) {
    var cssHref = ""

    var maskHref = ""

    var coverHref = ""
}

internal fun makeImpl20(book: Book, writer: VdmWriter, settings: Settings?): DataHolder {
    val data = Local(book, writer, settings)
    writeMetadata(data)
    writeContents(data)
    return data
}

private fun writeMetadata(data: Local) {
    val pkg = data.pkg
    val md = pkg.metadata
    val book = data.book

    md.attr["xmlns:opf"] = EPUB.XMLNS_OPF

    book[ISBN]?.toString()?.ifNotEmpty {
        md.addISBN("isbn", "urn:isbn:$it")
        pkg.uniqueIdentifier = "isbn"
    }
    book["uuid"]?.toString()?.ifNotEmpty {
        md.addUUID("uuid", "urn:uuid:$it")
        if (pkg.uniqueIdentifier.isEmpty()) {
            pkg.uniqueIdentifier = "uuid"
        }
    }
    if (pkg.uniqueIdentifier.isEmpty()) {
        md.addUUID("uuid", "urn:uuid:${UUID.randomUUID()}")
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
        data.coverHref = "../${data.writeFlob(it, "cover").href}"
        md.addMeta("cover", "cover")
    }
}

private fun writeContents(data: Local) {
    val resource = data.writeResource(data.getConfig("cssPath") ?: "!jem/format/epub/main.css", "style")
    data.cssHref = "../${resource.href}"

    val maskPath = data.writeResource(data.getConfig("maskPath") ?: "!jem/format/epub/mask.png", "mask")
    data.maskHref = "../$maskPath"


}
