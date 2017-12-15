package jem.format.epub.v2

import jem.format.epub.opf.DCME
import jem.format.epub.opf.Resource

class DCME2(name: String, text: String) : DCME(name, text) {
    val attrs = linkedMapOf<String, String>()
}

class Resource2(id: String, href: String, mediaType: String, fallback: String) : Resource(id, href, mediaType, fallback) {
    var fallbackStyle: String = ""
    var requiredNamespace: String = ""
    var requiredModules: String = ""
}
