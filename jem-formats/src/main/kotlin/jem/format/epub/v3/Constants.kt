package jem.format.epub.v3

const val MEDIA_TYPE_CSS = "text/css"
const val MEDIA_TYPE_XHTML = "application/xhtml+xml"

const val XML_NAMESPACE_OPF = "http://www.idpf.org/2007/opf"
const val XML_NAMESPACE_DCMES = "http://purl.org/dc/elements/1.1/"

val titleTypes = setOf(
        "main", "subtitle", "short", "collection", "edition", "expanded"
)

val allDcmes = setOf(
        "identifier", "language", "title", "contributor", "coverage", "creator", "date", "description", "format",
        "publisher", "relation", "rights", "source", "subject", "type"
)

// available for 'xml:lang', 'dir'
val localizableDcmes = setOf(
        "title", "contributor", "coverage", "creator", "description", "publisher", "relation", "rights", "subject"
)

val coreMediaTypes = setOf(
        "image/gif",
        "image/jpeg",
        "image/png",
        "image/svg+xml",
        "application/xhtml+xml",
        "application/javascript",
        "application/x-dtbncx+xml",
        "application/font-sfnt",
        "application/font-woff",
        "application/smil+xml",
        "application/pls+xml",
        "audio/mpeg",
        "audio/mp4",
        "text/css",
        "font/woff2"
)
