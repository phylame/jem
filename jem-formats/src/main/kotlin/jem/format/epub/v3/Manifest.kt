package jem.format.epub.v3

import jem.format.epub.EPUB

data class Resource(
        val id: String,
        val href: String,
        val mediaType: String,
        var fallback: String = "",
        var properties: String = "",
        var mediaOverlay: String = ""
)

class Manifest(val id: String = "") {
    internal val items = linkedMapOf<String, Resource>()

    fun addItem(item: Resource) {
        items[item.id] = item
    }

    fun remove(item: Resource) {
        items.remove(item.id)
    }
}

fun Manifest.addResource(id: String, href: String, mediaType: String, properties: String = "") =
        Resource(id, href, mediaType, properties = properties).also { addItem(it) }

fun Manifest.addNavigation(id: String, href: String): Resource =
        addResource(id, href, EPUB.MIME_XHTML, properties = EPUB.MANIFEST_NAVIGATION)

fun Manifest.addCoverImage(id: String, href: String, mediaType: String): Resource =
        addResource(id, href, mediaType, EPUB.MANIFEST_COVER_IMAGE)

