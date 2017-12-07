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
