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

import jclp.chooseAny
import jclp.io.HttpRequest
import jclp.io.actualStream
import jclp.io.openResource
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
val Settings?.userAgent inline get() = this?.getString("crawler.net.userAgent") ?: userAgents.chooseAny()
val Settings?.charset inline get() = this?.getString("crawler.net.charset") ?: "UTF-8"

inline fun <R> connectLoop(url: String, settings: Settings?, block: () -> R): R {
    for (i in 1..settings.connectTimes) {
        if (Thread.interrupted()) throw InterruptedIOException()
        try {
            return block()
        } catch (ignored: SocketTimeoutException) {
        }
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
