package jem.format.epub.v3

import jclp.text.or
import jclp.utcISODateTime
import java.time.OffsetDateTime
import java.util.*
import kotlin.collections.LinkedHashMap

sealed class Item(id: String) {
    companion object {
        private var counter = 0
    }

    val id: String = id or { "e${++counter}" }

    var textDirection: String = ""

    var language: String = ""

    override fun toString() = "language='$language', textDirection='$textDirection', id='$id'"
}

class DCME(val name: String, text: String, id: String = "") : Item(id) {
    var text = text
        set(value) {
            require(value.isNotEmpty()) { "'text' cannot be empty" }
            field = value
        }

    init {
        require(name.isNotEmpty()) { "'name' cannot be empty" }
    }

    override fun toString() = "DCME(name='$name', text='$text', ${super.toString()})"
}

class Meta(val property: String, text: String, var scheme: String = "", val refines: String = "", id: String = "") : Item(id) {
    var text = text
        set(value) {
            require(value.isNotEmpty()) { "'text' cannot be empty" }
            field = value
        }

    init {
        require(property.isNotEmpty()) { "'property' cannot be empty" }
    }

    override fun toString() =
            "Meta(property='$property', text='$text', refines='$refines', scheme='$scheme', ${super.toString()})"
}

class Link(val rel: String, href: String, val refines: String = "", var mediaType: String = "", id: String = "") : Item(id) {
    var href = href
        set(value) {
            require(value.isNotEmpty()) { "'href' cannot be empty" }
            field = value
        }

    init {
        require(rel.isNotEmpty()) { "'rel' cannot be empty" }
    }

    override fun toString() =
            "Link(rel='$rel', href='$href', refines='$refines', mediaType='$mediaType', ${super.toString()})"
}

class Metadata {
    internal val items = LinkedHashMap<String, Item>()

    fun addItem(item: Item) {
        items[item.id] = item
    }

    fun removeItem(item: Item) {
        items.remove(item.id)
    }

    fun addDCME(name: String, text: String, id: String = "") = DCME(name, text, id).also { addItem(it) }

    fun addDCME(name: String, text: String, property: String, meta: String, scheme: String = "", id: String = ""): DCME
            = addDCME(name, text, id).also { addMeta(property, meta, scheme, "#${it.id}") }

    fun addMeta(property: String, text: String, scheme: String = "", refines: String = "", id: String = ""): Meta
            = Meta(property, text, scheme, refines, id).also { addItem(it) }

    fun addLink(rel: String, href: String, mediaType: String = "", refines: String = "", id: String = ""): Link =
            Link(rel, href, refines, mediaType, id).also { addItem(it) }
}

fun Metadata.addAlternateText(refId: String, name: String, language: Locale): Meta
        = addMeta("alternate-script", name, refines = "#$refId").also { it.language = language.toLanguageTag() }

fun Metadata.addNormalizedText(refId: String, name: String): Meta
        = addMeta("file-as", name, refines = "#$refId")

fun Metadata.addIdentifier(identifier: String, type: String = "", scheme: String = "", id: String = ""): DCME
        = addDCME("identifier", identifier, "identifier-type", type, scheme, id)

fun Metadata.addDOI(id: String, doi: String): DCME
        = addIdentifier(doi, "06", "onix:codelist5", id)

fun Metadata.addISBN(id: String, isbn: String): DCME
        = addIdentifier(isbn, "15", "onix:codelist5", id)

fun Metadata.addUUID(id: String, uuid: String): DCME
        = addIdentifier(uuid, "uuid", "xsd:string", id)

fun Metadata.addModifiedTime(time: OffsetDateTime, id: String = ""): Meta
        = addMeta("dcterms:modified", time.format(utcISODateTime), id)

fun Metadata.addTitle(title: String, type: String = "", displaySeq: Int = 0, groupPosition: Int = 0, id: String = ""): DCME {
    if (groupPosition > 0) {
        require(type == "collection") { "'type' is not 'collection'" }
    }
    if (type.isEmpty()) {
        return addDCME("title", title)
    }
    return addDCME("title", title, "title-type", type, id = id).also {
        if (groupPosition > 0) {
            addMeta("group-position", groupPosition.toString(), refines = "#${it.id}")
        }
        if (displaySeq > 0) {
            addMeta("display-seq", displaySeq.toString(), refines = "#${it.id}")
        }
    }
}

fun Metadata.addCreator(name: String, role: String = "", scheme: String = "", displaySeq: Int = 0, id: String = ""): DCME {
    return addDCME("creator", name, "role", role, scheme, id).also {
        if (displaySeq > 0) {
            addMeta("display-seq", displaySeq.toString(), refines = "#${it.id}")
        }
    }
}

fun Metadata.addAuthor(author: String, displaySeq: Int = 0, id: String = ""): DCME
        = addCreator(author, "aut", "marc:relators", displaySeq, id)

fun Metadata.addLanguage(language: Locale): DCME
        = addDCME("language", language.toLanguageTag())

fun Metadata.addPubdate(date: OffsetDateTime, id: String = ""): DCME
        = addDCME("date", date.format(utcISODateTime), id)

fun Metadata.addAuthority(refId: String, name: String, id: String = ""): Meta
        = addMeta("meta-auth", name, refines = "#$refId", id = id)

fun Metadata.addMarc21Record(href: String, id: String = ""): Link
        = addLink("marc21xml-record", href = href, id = id)

fun Metadata.addModsRecord(href: String, id: String = ""): Link
        = addLink("mods-record", href = href, id = id)

fun Metadata.addOnixRecord(href: String, id: String = ""): Link
        = addLink("onix-record", href = href, id = id)

fun Metadata.addXmlSignature(refId: String, href: String, id: String = ""): Link
        = addLink("xml-signature", href = href, refines = "#$refId", id = id)

fun Metadata.addXmpRecord(href: String, id: String = ""): Link
        = addLink("xmp-record", href = href, id = id)
