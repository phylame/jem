package jem.format.epub.opf

import java.util.*

open class ItemRef(val idref: String, var linear: Boolean, var properties: String)

open class Spine(val id: String, var toc: String) {
    val refs = LinkedList<ItemRef>()
}
