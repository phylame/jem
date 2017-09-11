package jclp.io

import java.util.*

const val UNKNOWN_MIME = "application/octet-stream"

private val mimeMap by lazy {
    getProperties("!jclp/io/mime.properties") ?: Properties()
}

fun mapMime(extension: String, mime: String) {
    mimeMap[extension] = mime
}

fun mapMimes(m: Map<String, String>) {
    mimeMap.putAll(m)
}

fun getMime(path: String) = if (!path.isEmpty()) extName(path).let {
    if (it.isEmpty()) UNKNOWN_MIME else mimeMap.getProperty(it) ?: UNKNOWN_MIME
} else {
    ""
}

fun detectMime(path: String, mime: String) = if (mime.isNotEmpty()) mime else getMime(path)
