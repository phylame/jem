package jem.format.epub.v3

const val PROPERTIES_NAVIGATION = "nav"
const val PROPERTIES_COVER_IMAGE = "cover-image"

data class Resource(
        val id: String,
        val href: String,
        val mediaType: String,
        var fallback: String = "",
        var properties: String = "",
        var mediaOverlay: String = ""
)

class Manifest(val id: String = "") {
    internal val items = LinkedHashMap<String, Resource>()

    fun addItem(item: Resource) {
        items[item.id] = item
    }

    fun remove(item: Resource) {
        items.remove(item.id)
    }
}

fun Manifest.addNavigation(id: String, href: String): Resource =
        Resource(id, href, MEDIA_TYPE_XHTML, properties = PROPERTIES_NAVIGATION).also { addItem(it) }

fun Manifest.addCoverImage(id: String, href: String, mediaType: String): Resource =
        Resource(id, href, mediaType, properties = PROPERTIES_COVER_IMAGE).also { addItem(it) }
