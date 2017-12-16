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

package jem.format.epub.opf

import jclp.text.ifNotEmpty
import jclp.xml.attribute
import jclp.xml.endTag
import jclp.xml.startTag
import jem.format.epub.Taggable
import org.xmlpull.v1.XmlSerializer
import java.util.*

class Resource(id: String, var href: String, var mediaType: String) : Taggable(id) {
    override fun renderTo(xml: XmlSerializer) {
        with(xml) {
            startTag("item")
            attribute("id", id)
            attribute("href", href)
            attribute("media-type", mediaType)
            attr.forEach { attribute(it.key, it.value) }
            endTag()
        }
    }
}

class Manifest(id: String = "") : Taggable(id) {
    private val items = LinkedList<Resource>()

    operator fun plusAssign(item: Resource) {
        items += item
    }

    operator fun minusAssign(item: Resource) {
        items -= item
    }

    fun addResource(id: String, href: String, mediaType: String) = Resource(id, href, mediaType).also { items += it }

    override fun renderTo(xml: XmlSerializer) {
        with(xml) {
            startTag("manifest")
            id.ifNotEmpty { attribute("id", it) }
            items.forEach { it.renderTo(this) }
            endTag()
        }
    }
}
