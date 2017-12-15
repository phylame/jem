package jem.format.epub.opf

sealed class Item

open class DCME(val name: String, var text: String) : Item()

open class Meta(val name: String, val text: String) : Item()

class Metadata {
    val items = linkedMapOf<String, Item>()
}
