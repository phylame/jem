/*
 * Copyright 2017 Peng Wan <phylame@163.com>
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

package jem.format.epub

import jem.format.util.XmlRender
import java.util.*

private const val OPF_XMLNS = "http://www.idpf.org/2007/opf"

data class Dublin(
        val name: String,
        val content: String,
        val id: String = "",
        val role: String = "",
        val event: String = "",
        val schema: String = ""
)

data class Meta(val name: String, val content: String, val property: String = "")

data class Item(val id: String, val href: String, val type: String)

data class Spine(val idref: String, val linear: Boolean, val properties: String)

data class Guide(val href: String, val type: String, val title: String)

class OPFData {
    var bookId = ""

    val metadata = LinkedList<Any>()

    val resources = LinkedList<Item>()

    val guides = LinkedList<Guide>()

    val spines = LinkedList<Spine>()
}

fun renderOPFv2(render: XmlRender, ncxId: String, data: OPFData) = with(render) {
    beginXml()

    beginTag("package")
            .attribute("version", "2.0")
            .attribute("unique-identifier", data.bookId)
            .xmlns(OPF_XMLNS)

    writeMetadata(render, data.metadata)

    beginTag("manifest")
    for ((id, href, type) in data.resources) {
        beginTag("item")
        attribute("id", id)
        attribute("href", href)
        attribute("media-type", type)
        endTag()
    }
    endTag()

    beginTag("spine").attribute("toc", ncxId)
    for ((idref, linear, properties) in data.spines) {
        beginTag("itemref")
        attribute("idref", idref)
        if (!linear) {
            attribute("linear", "no")
        }
        if (properties.isNotEmpty()) {
            attribute("properties", properties)
        }
        endTag()
    }
    endTag()

    beginTag("guide")
    for ((href, type, title) in data.guides) {
        beginTag("reference")
        attribute("href", href)
        attribute("type", type)
        attribute("title", title)
        endTag()
    }
    endTag()

    endTag()
    endXml()
}

private fun writeMetadata(render: XmlRender, metadata: List<Any>) = with(render) {
    beginTag("metadata")
    attribute("xmlns:dc", "http://purl.org/dc/elements/1.1/")
    attribute("xmlns:opf", OPF_XMLNS)
    for (item in metadata) {
        when (item) {
            is Dublin -> {
                with(item) {
                    beginTag("dc:$name")
                    if (id.isNotEmpty()) {
                        attribute("id", id)
                    }
                    if (role.isNotEmpty()) {
                        attribute("opf:role", role)
                    }
                    if (event.isNotEmpty()) {
                        attribute("opf:event", event)
                    }
                    if (schema.isNotEmpty()) {
                        attribute("opf:schema", schema)
                    }
                    text(content)
                    endTag()
                }
            }
            is Meta -> {
                beginTag("meta")
                if (item.property.isNotEmpty()) {
                    attribute("property", item.property)
                    text(item.content)
                } else {
                    attribute("name", item.name)
                    attribute("content", item.content)
                }
                endTag()
            }
        }
    }
    endTag()
}
