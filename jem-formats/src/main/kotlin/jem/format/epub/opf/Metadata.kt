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
import jem.format.epub.EPUB
import jem.format.epub.Taggable
import org.xmlpull.v1.XmlSerializer
import java.util.*

sealed class Item(id: String) : Taggable(id)

class DCME(val name: String, var text: String, id: String) : Item(id) {
    override fun renderTo(xml: XmlSerializer) {
        with(xml) {
            startTag("dc:${this@DCME.name}")
            attr.forEach { attribute(it.key, it.value) }
            id.ifNotEmpty { attribute("id", it) }
            text(text)
            endTag()
        }
    }
}

abstract class Meta(val key: String, val content: String, id: String) : Item(id)

class Metadata : Taggable("") {
    private val items = LinkedList<Item>()

    operator fun plusAssign(item: Item) {
        items += item.apply {
            if (id == "-") id = "e${items.size + 1}"
        }
    }

    operator fun minusAssign(item: Item) {
        items -= item
    }

    fun addDCME(name: String, text: String, id: String = "") =
            DCME(name, text, id).also { items += it }

    override fun renderTo(xml: XmlSerializer) {
        with(xml) {
            startTag("dc", EPUB.XMLNS_DCME, "metadata")
            attr.forEach { attribute(it.key, it.value) }
            items.forEach { it.renderTo(this) }
            endTag()
        }
    }

}
