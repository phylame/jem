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

package jem.format.epub.v3

import jclp.text.ifNotEmpty
import jclp.text.or
import jclp.xml.attribute
import jclp.xml.endTag
import jclp.xml.startTag
import jem.format.epub.opf.DCME
import jem.format.epub.opf.Meta
import jem.format.epub.opf.Metadata
import org.xmlpull.v1.XmlSerializer
import java.time.Instant
import java.time.temporal.ChronoField
import java.util.*

fun Metadata.addDCME(name: String, text: String, property: String, meta: String, scheme: String = "", id: String = "") =
        addDCME(name, text, id or "-").also { addMeta(property, meta, scheme, "#${it.id}") }

class Meta3(
        property: String, text: String, var scheme: String = "", val refines: String = "", id: String = ""
) : Meta(property, text, id) {
    override fun renderTo(xml: XmlSerializer) {
        with(xml) {
            startTag("meta")
            refines.ifNotEmpty { attribute("refines", it) }
            attribute("property", key)
            scheme.ifNotEmpty { attribute("scheme", it) }
            attr.forEach { attribute(it.key, it.value) }
            text(content)
            endTag()
        }
    }
}

fun Metadata.addMeta(property: String, content: String, scheme: String = "", refines: String = "", id: String = "") =
        Meta3(property, content, scheme, refines, id).also { this += it }

fun Metadata.addAlternateText(refId: String, name: String, language: Locale): Meta3
        = addMeta("alternate-script", name, refines = "#$refId").also { it.attr["xml:lang"] = language.toLanguageTag() }

fun Metadata.addNormalizedText(refId: String, name: String): Meta3
        = addMeta("file-as", name, refines = "#$refId")

fun Metadata.addIdentifier(identifier: String, type: String = "", scheme: String = "", id: String = ""): DCME
        = addDCME("identifier", identifier, "identifier-type", type, scheme, id)

fun Metadata.addDOI(id: String, doi: String): DCME
        = addIdentifier(doi, "06", "onix:codelist5", id)

fun Metadata.addISBN(id: String, isbn: String): DCME =
        if (isbn.length == 13) {
            addIdentifier(isbn, "02", "onix:codelist5", id)
        } else {
            addIdentifier(isbn, "15", "onix:codelist5", id)
        }

fun Metadata.addUUID(id: String, uuid: String): DCME
        = addIdentifier(uuid, "uuid", "xsd:string", id)

fun Metadata.addModifiedTime(time: Instant, id: String = ""): Meta3
        = addMeta("dcterms:modified", time.with(ChronoField.MILLI_OF_SECOND, 0).toString(), id)

fun Metadata.addTitle(title: String, type: String = "", displaySeq: Int = 0, groupPosition: Int = 0, id: String = "-"): DCME {
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

fun Metadata.addCreator(name: String, role: String = "", scheme: String = "", displaySeq: Int = 0, id: String = "-"): DCME {
    return addDCME("creator", name, "role", role, scheme, id).also {
        if (displaySeq > 0) {
            addMeta("display-seq", displaySeq.toString(), refines = "#${it.id}")
        }
    }
}

fun Metadata.addAuthor(author: String, displaySeq: Int = 0, id: String = "-"): DCME
        = addCreator(author, "aut", "marc:relators", displaySeq, id)

fun Metadata.addVendor(author: String, displaySeq: Int = 0, id: String = "-"): DCME
        = addCreator(author, "bkp", "marc:relators", displaySeq, id)

fun Metadata.addPubdate(date: Instant, id: String = ""): DCME
        = addDCME("date", date.with(ChronoField.MILLI_OF_SECOND, 0).toString(), id)
