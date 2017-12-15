package jem.format.epub.opf

open class Resource(val id: String, var href: String, var mediaType: String, var fallback: String)

class Manifest {
    val items = linkedMapOf<String, Resource>()
}
