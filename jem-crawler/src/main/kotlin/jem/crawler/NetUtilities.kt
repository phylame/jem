package jem.crawler

import jclp.choose
import jclp.io.HttpRequest
import jclp.io.openResource
import jclp.io.openStream
import jclp.setting.Settings
import jclp.setting.getInt
import jclp.setting.getString
import jclp.text.valueFor
import java.io.InterruptedIOException
import java.io.Reader
import java.net.SocketTimeoutException
import java.net.URLConnection
import java.nio.charset.Charset

val userAgents by lazy {
    openResource("!jem/crawler/agents.txt")?.reader()?.useLines(Sequence<String>::toList) ?: emptyList()
}

val Settings?.connectTimes inline get() = this?.getInt("crawler.net.tryTimes") ?: 3
val Settings?.connectTimeout inline get() = this?.getInt("crawler.net.timeout") ?: 5000
val Settings?.userAgent inline get() = this?.getString("crawler.net.userAgent") ?: userAgents.choose()
val Settings?.charset inline get() = this?.getString("crawler.net.charset") ?: "UTF-8"

inline fun <R> connectLoop(url: String, settings: Settings?, block: () -> R): R {
    for (i in 1..settings.connectTimes) {
        if (Thread.interrupted()) {
            throw InterruptedIOException()
        }
        try {
            return block()
        } catch (ignored: SocketTimeoutException) {
        }
    }
    throw SocketTimeoutException("Timeout for $url")
}

fun openConnection(url: String, method: String, settings: Settings?): URLConnection {
    val request = HttpRequest(url, method).apply {
        properties["User-Agent"] = settings.userAgent
        properties["Accept-Encoding"] = "gzip,deflate"
        connectTimeout = settings.connectTimeout
    }
    return connectLoop(url, settings) {
        request.connect()
    }
}

fun URLConnection.openReader(settings: Settings?): Reader {
    val encoding = contentType?.valueFor("charset", ";") ?: settings.charset
    return openStream().reader(Charset.forName(encoding))
}