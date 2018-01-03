package jem.crawler

import jclp.io.htmlTrim
import jclp.io.openResource
import jclp.text.ifNotEmpty
import org.json.JSONObject
import org.json.JSONTokener
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements
import java.io.Reader

fun Element.joinText(separator: String, ignoredTags: List<String> = emptyList()): String {
    val b = StringBuilder()
    val end = childNodeSize()
    childNodes().forEachIndexed { index, node ->
        if (node.nodeName() !in ignoredTags) {
            val text = when (node) {
                is TextNode -> node.text().htmlTrim()
                is Element -> node.text().htmlTrim()
                else -> ""
            }
            if (text.isNotEmpty()) {
                b.append(text)
                if (index != end) {
                    b.append(separator)
                }
            }
        }
    }
    return b.toString()
}

fun Elements.joinText(separator: String, ignoredTags: List<String> = emptyList()): String {
    val end = size
    val b = StringBuilder()
    forEachIndexed { index, element ->
        element.joinText(separator).ifNotEmpty {
            b.append(it)
            if (index != end) {
                b.append(separator)
            }
        }
    }
    return b.toString()
}

data class ItemSelector(val key: String, val type: String, val query: String) {
    fun getValue(parent: Element): String {
        val node = if (query.isNotEmpty()) parent.selectFirst(query) else parent
        return when {
            type.startsWith("$") -> node.attr(type.substring(1))
            type == "own" -> node.ownText()
            else -> node.text()
        }
    }
}

data class SelectorList(val base: String, val selectors: MutableList<ItemSelector>)

fun loadSelectors(json: JSONObject) = SelectorList(json.optString("list"), arrayListOf()).apply {
    for (pattern in json.getJSONArray("patterns")) {
        (pattern as String).split(";").let {
            selectors += ItemSelector(it[0], it[1], if (it.size > 2) it[2] else "")
        }
    }
}

fun loadSelectors(reader: Reader) = loadSelectors(JSONObject(JSONTokener(reader)))

fun loadSelectors(path: String, loader: ClassLoader? = null): SelectorList? =
        openResource(path, loader)?.reader()?.use { loadSelectors(it) }
