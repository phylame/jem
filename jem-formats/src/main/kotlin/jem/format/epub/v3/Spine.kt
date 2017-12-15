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

import java.util.*

const val PROPERTIES_PAGE_SPREAD_LEFT = "page-spread-left"
const val PROPERTIES_PAGE_SPREAD_RIGHT = "page-spread-right"

data class ItemRef(val idref: String, var linear: Boolean = true, var properties: String = "", val id: String = "")

class Spine(val id: String = "", var toc: String = "", var pageProgressionDirection: String = "") {
    internal val refs = LinkedList<ItemRef>()

    fun addItem(ref: ItemRef) {
        refs += ref
    }

    fun removeItem(ref: ItemRef) {
        refs -= ref
    }

    fun addReference(idref: String, linear: Boolean = true, properties: String = "", id: String = ""): ItemRef
            = ItemRef(idref, linear, properties, id).also { addItem(it) }
}
