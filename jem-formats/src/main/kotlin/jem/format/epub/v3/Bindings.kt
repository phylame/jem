package jem.format.epub.v3

data class MediaType(val handler: String, val mediaType: String) {
    init {
        require(handler.isNotEmpty()) { "'handler' cannot be empty" }
        require(mediaType.isNotEmpty()) { "'mediaType' cannot be empty" }
    }
}

class Bindings {
    internal val items = arrayListOf<MediaType>()

    fun addItem(item: MediaType) {
        items += item
    }

    fun removeItem(item: MediaType) {
        items -= item
    }

    fun addMediaType(handler: String, mediaType: String): MediaType
            = MediaType(handler, mediaType).also { addItem(it) }
}
