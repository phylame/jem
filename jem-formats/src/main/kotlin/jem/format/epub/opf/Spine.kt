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

class ItemRef(val idref: String, var linear: Boolean, var properties: String, id: String = "") : Taggable(id) {
    override fun renderTo(xml: XmlSerializer) {
        with(xml) {
            startTag("itemref")
            attribute("idref", idref)
            if (!linear) attribute("linear", "no")
            properties.ifNotEmpty { attribute("properties", it) }
            attr.forEach { attribute(it.key, it.value) }
            id.ifNotEmpty { attribute("id", it) }
            endTag()
        }
    }
}

class Spine(var toc: String = "", id: String = "") : Taggable(id) {
    private val refs = LinkedList<ItemRef>()

    operator fun plusAssign(ref: ItemRef) {
        refs += ref
    }

    operator fun minusAssign(ref: ItemRef) {
        refs -= ref
    }

    fun addRef(idref: String, linear: Boolean = true, properties: String = "", id: String = "") =
            ItemRef(idref, linear, properties, id).also { refs += it }

    override fun renderTo(xml: XmlSerializer) {
        with(xml) {
            startTag("spine")
            toc.ifNotEmpty { attribute("toc", it) }
            attr.forEach { attribute(it.key, it.value) }
            id.ifNotEmpty { attribute("id", it) }
            refs.forEach { it.renderTo(this) }
            endTag()
        }
    }
}
