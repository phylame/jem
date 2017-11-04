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

import jclp.Linguist
import jclp.io.Flob
import jclp.io.actualStream
import jclp.io.htmlTrim
import jclp.io.mimeType
import jclp.setting.Settings
import jclp.text.or
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements

internal object M : Linguist("!jem/crawler/messages")

fun fetchSoup(url: String, method: String, settings: Settings?): Document = connectLoop(url, settings) {
    Jsoup.connect(url)
            .userAgent(settings.userAgent)
            .timeout(settings.connectTimeout)
            .header("Accept-Encoding", "gzip,deflate").let {
        if (method.equals("get", true)) it.get() else it.post()
    }
}

fun fetchJson(url: String, method: String, settings: Settings?) = openConnection(url, method, settings).let {
    JSONObject(it.openReader(settings).use { it.readText() })
}

class CrawlerFlob(
        private val url: String, private val method: String, private val settings: Settings?, mime: String = ""
) : Flob {
    override val name = url

    override val mimeType = mime or { mimeType(name) }

    override fun openStream() = openConnection(url, method, settings).actualStream()
}

fun Element.selectText(query: String, separator: String = "") = select(query).joinText(separator)

fun Elements.selectText(query: String, separator: String = "") = select(query).joinText(separator)

fun Element.selectImage(query: String) = select(query).firstOrNull()?.absUrl("src")

fun Elements.selectImage(query: String) = select(query).firstOrNull()?.absUrl("src")

fun Element.subText(index: Int): String {
    var i = 0
    return childNodes()
            .filterIsInstance<TextNode>()
            .map { it.text().htmlTrim() }
            .firstOrNull { it.isNotEmpty() && index == i++ }
            ?: ""
}

fun Element.joinText(separator: String) = childNodes().map {
    when (it) {
        is TextNode -> it.text().htmlTrim()
        is Element -> it.text().htmlTrim()
        else -> ""
    }
}.filter(String::isNotEmpty).joinToString(separator)

fun Elements.joinText(separator: String) = map {
    it.joinText(separator)
}.joinToString(separator)
