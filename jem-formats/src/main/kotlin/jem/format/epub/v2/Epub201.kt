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

package jem.format.epub.v2

import jclp.xml.attribute
import jclp.xml.endTag
import jclp.xml.startTag
import jem.format.epub.opf.Meta
import jem.format.epub.opf.Metadata
import org.xmlpull.v1.XmlSerializer
import java.time.Instant
import java.time.temporal.ChronoField

class Meta2(name: String, content: String, id: String = "") : Meta(name, content, id) {
    override fun renderTo(xml: XmlSerializer) {
        with(xml) {
            startTag("meta")
            attribute("name", key)
            attribute("content", content)
            endTag()
        }
    }
}

fun Metadata.addMeta(name: String, content: String, id: String = "") =
        Meta2(name, content, id).also { this += it }

fun Metadata.addDOI(id: String, doi: String) {
    addDCME("identifier", doi, id).apply {
        attr["opf:scheme"] = "DOI"
    }
}

fun Metadata.addISBN(id: String, isbn: String) {
    addDCME("identifier", isbn, id).apply {
        attr["opf:scheme"] = "ISBN"
    }
}

fun Metadata.addUUID(id: String, uuid: String) {
    addDCME("identifier", uuid, id).apply {
        attr["opf:scheme"] = "UUID"
    }
}

fun Metadata.addModifiedTime(time: Instant) {
    addDCME("date", time.with(ChronoField.MILLI_OF_SECOND, 0).toString()).apply {
        attr["opf:event"] = "modification"
    }
}

fun Metadata.addAuthor(author: String, fileAs: String = "") {
    addDCME("creator", author).apply {
        attr["opf:role"] = "aut"
        if (fileAs.isNotEmpty()) {
            attr["opf:file-as"] = fileAs
        }
    }
}

fun Metadata.addVendor(vendor: String, fileAs: String = "") {
    addDCME("contributor", vendor).apply {
        attr["opf:role"] = "bkp"
        if (fileAs.isNotEmpty()) {
            attr["opf:file-as"] = fileAs
        }
    }
}

fun Metadata.addPubdate(date: Instant) {
    addDCME("date", date.with(ChronoField.MILLI_OF_SECOND, 0).toString()).apply {
        attr["opf:event"] = "publication"
    }
}
