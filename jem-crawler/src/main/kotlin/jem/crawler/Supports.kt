package jem.crawler

import jclp.Linguist
import jclp.flob.AbstractFlob
import jclp.io.openStream
import jclp.io.unquote
import jclp.setting.Settings
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
) : AbstractFlob(mime) {
    override val name = url

    override fun openStream() = openConnection(url, method, settings).openStream()
}

fun Element.selectText(query: String, separator: String = "") = select(query).joinText(separator)

fun Elements.selectText(query: String, separator: String = "") = select(query).joinText(separator)

fun Element.selectImage(query: String) = select(query).firstOrNull()?.absUrl("src")

fun Elements.selectImage(query: String) = select(query).firstOrNull()?.absUrl("src")

fun Element.subText(index: Int): String {
    var i = 0
    return childNodes()
            .filterIsInstance<TextNode>()
            .map { it.text().unquote() }
            .firstOrNull { it.isNotEmpty() && index == i++ }
            ?: ""
}

fun Element.joinText(separator: String) = childNodes().map {
    when (it) {
        is TextNode -> it.text().unquote()
        is Element -> it.text().unquote()
        else -> ""
    }
}.filter(String::isNotEmpty).joinToString(separator)

fun Elements.joinText(separator: String) = map {
    it.joinText(separator)
}.joinToString(separator)
