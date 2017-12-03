/*
 * Copyright 2015-2017 Peng Wan <phylame@163.com>
 *
 * This file is part of Jem.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jem.crawler

import jclp.AsyncTask
import jclp.Linguist
import jclp.chooseAny
import jclp.io.*
import jclp.log.Log
import jclp.setting.Settings
import jclp.setting.getInt
import jclp.setting.getString
import jclp.text.TEXT_PLAIN
import jclp.text.Text
import jclp.text.or
import jclp.text.valueFor
import jem.Book
import jem.Chapter
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements
import java.io.InterruptedIOException
import java.io.Reader
import java.net.SocketTimeoutException
import java.net.URLConnection
import java.nio.charset.Charset
import java.util.concurrent.ExecutorService

const val CRAWLER_LISTENER_KEY = "crawler.listener"

internal object M : Linguist("!jem/crawler/messages")

val userAgents by lazy {
    openResource("!jem/crawler/agents.txt")?.reader()?.useLines(Sequence<String>::toList) ?: emptyList()
}

val Settings?.connectTimes inline get() = this?.getInt("crawler.net.tryTimes") ?: 3
val Settings?.connectTimeout inline get() = this?.getInt("crawler.net.timeout") ?: 5000
val Settings?.userAgent inline get() = this?.getString("crawler.net.userAgent") ?: userAgents.chooseAny()
val Settings?.charset inline get() = this?.getString("crawler.net.charset") ?: "UTF-8"

inline fun <R> connectLoop(url: String, settings: Settings?, block: () -> R): R {
    for (i in 1..settings.connectTimes) {
        if (Thread.interrupted()) throw InterruptedIOException()
        try {
            return block()
        } catch (ignored: SocketTimeoutException) {
        }
        Log.t("connectLoop") { "try reconnect to $url: ${i + 1}" }
    }
    throw SocketTimeoutException("Timeout for $url")
}

fun openConnection(url: String, method: String, settings: Settings?): URLConnection {
    val request = HttpRequest(url, method).apply {
        isDoInput = true
        properties["User-Agent"] = settings.userAgent
        properties["Accept-Encoding"] = "gzip,deflate"
        connectTimeout = settings.connectTimeout
    }
    return connectLoop(url, settings) { request.connect() }
}

fun URLConnection.openReader(settings: Settings?): Reader {
    val encoding = contentType?.valueFor("charset", ";") ?: settings.charset
    return actualStream().reader(Charset.forName(encoding))
}

fun fetchSoup(url: String, method: String, settings: Settings?): Document =
        connectLoop(url, settings) {
            with(Jsoup.connect(url)) {
                userAgent(settings.userAgent)
                timeout(settings.connectTimeout)
                header("Accept-Encoding", "gzip,deflate")
                if (method.equals("get", true)) get() else post()
            }
        }

fun fetchJson(url: String, method: String, settings: Settings?) =
        openConnection(url, method, settings).openReader(settings).use { JSONObject(it.readText()) }

fun fetchString(url: String, method: String, settings: Settings?) =
        openConnection(url, method, settings).openReader(settings).use { it.readText() }

class CrawlerBook() : Book() {
    var sourceUrl
        inline get() = extensions[EXT_CRAWLER_SOURCE_URL] ?: ""
        inline set(value) {
            extensions[EXT_CRAWLER_SOURCE_URL] = value
        }

    var sourceSite
        inline get() = extensions[EXT_CRAWLER_SOURCE_SITE] ?: ""
        inline set(value) {
            extensions[EXT_CRAWLER_SOURCE_SITE] = value
        }

    var bookId
        inline get() = extensions[EXT_CRAWLER_BOOK_ID] ?: ""
        inline set(value) {
            extensions[EXT_CRAWLER_BOOK_ID] = value
        }

    var lastChapter
        inline get() = extensions[EXT_CRAWLER_LAST_CHAPTER] ?: ""
        inline set(value) {
            extensions[EXT_CRAWLER_LAST_CHAPTER] = value
        }

    var updateTime
        inline get() = extensions[EXT_CRAWLER_UPDATE_TIME]
        inline set(value) {
            extensions[EXT_CRAWLER_UPDATE_TIME] = value!!
        }

}

class CrawlerFlob(
        private val url: String, private val method: String, private val settings: Settings?, name: String = "", mime: String = ""
) : Flob {
    override val name = name or url

    override val mimeType = mime or { mimeType(this.name) }

    override fun openStream() = openConnection(url, method, settings).actualStream()

    override fun toString(): String = "$url;mime=$mimeType"
}

interface TextListener {
    fun onStart(url: String, chapter: Chapter) {}

    fun onSuccess(url: String, chapter: Chapter) {}

    fun onError(t: Throwable, url: String, chapter: Chapter) {}
}

class CrawlerText(val url: String, val chapter: Chapter, val crawler: Crawler, val settings: Settings?) : Text {
    override val type = TEXT_PLAIN

    override fun toString() = task.get()

    fun schedule(executor: ExecutorService) {
        task.schedule(executor)
    }

    private val listener = settings?.get(CRAWLER_LISTENER_KEY) as? TextListener

    private val task = object : AsyncTask<String>() {
        override fun handleGet(): String {
            listener?.onStart(url, chapter)
            return try {
                crawler.getText(url, settings).also { listener?.onSuccess(url, chapter) }
            } catch (e: Exception) {
                Log.e("CrawlerText", e) { "Cannot fetch text from $url" }
                listener?.onError(e, url, chapter)
                ""
            }
        }
    }
}

fun Elements.selectText(query: String, separator: String = "") = select(query).joinText(separator)

fun Element.selectText(query: String, separator: String = "") = select(query).joinText(separator)

fun Elements.selectImage(query: String) = select(query).firstOrNull()?.absUrl("src")

fun Element.selectImage(query: String) = selectFirst(query)?.absUrl("src")

fun Element.subText(index: Int): String {
    var i = 0
    return textNodes()
            .map { it.text().htmlTrim() }
            .firstOrNull { it.isNotEmpty() && index == i++ }
            ?: ""
}

fun Element.joinText(separator: String) =
        childNodes().map {
            when (it) {
                is TextNode -> it.text().htmlTrim()
                is Element -> it.text().htmlTrim()
                else -> ""
            }
        }.filter {
            it.isNotEmpty()
        }.joinToString(separator)

fun Elements.joinText(separator: String) =
        map { it.joinText(separator) }.joinToString(separator)
