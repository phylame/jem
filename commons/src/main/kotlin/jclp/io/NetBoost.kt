package jclp.io

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.net.URLEncoder
import java.util.zip.GZIPInputStream

fun String.htmlTrim() = trim().replace("\u00A0", "")

fun String.quote(encoding: String = "UTF-8"): String = URLEncoder.encode(this, encoding)

fun Map<String, String>.joinToQuery(encoding: String = "UTF-8"): String {
    return entries.joinToString("&") { "${it.key.quote(encoding)}=${it.value.quote(encoding)}" }
}

fun URLConnection.openStream(): InputStream {
    return if (getHeaderField("Content-Encoding")?.contains("gzip", true) == true) {
        GZIPInputStream(getInputStream())
    } else {
        getInputStream()
    }
}

data class HttpRequest(private val url: String, private val method: String = "GET") {
    var encoding = "UTF-8"

    var connectTimeout: Int = 0
    var readTimeout: Int = 0

    var isDoInput = false
    var isDoOutput = false
    var isUseCaches = false

    var payload: ByteArray? = null

    val parameters = hashMapOf<String, String>()

    val properties = hashMapOf<String, String>()

    fun connect(): URLConnection {
        val isHttp = url.startsWith("http")
        val path = if (isHttp && parameters.isNotEmpty()) {
            if (method.equals("post", true)) {
                TODO("generate http post form")
            } else {
                url + "?" + parameters.joinToQuery(encoding)
            }
        } else url
        return URL(path).openConnection().apply {
            if (this is HttpURLConnection) {
                this.requestMethod = method.toUpperCase()
            }
            doInput = isDoInput
            doOutput = isDoOutput
            useCaches = isUseCaches
            connectTimeout = this@HttpRequest.connectTimeout
            readTimeout = this@HttpRequest.readTimeout
            for ((key, value) in properties) {
                setRequestProperty(key, value)
            }
            if (payload?.isNotEmpty() == true) {
                getOutputStream().use {
                    it.write(payload)
                    it.flush()
                }
            }
            connect()
        }
    }
}
