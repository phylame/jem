package jem.format.epub.html

import jclp.io.Flob
import jclp.isNotRoot
import jclp.text.Text
import jclp.text.ifNotEmpty
import jclp.xml.Tag
import jem.Chapter
import jem.cover
import jem.format.epub.DataHolder
import jem.format.epub.EPUB
import jem.format.epub.opf.Resource
import jem.format.util.xmlDsl
import jem.intro
import jem.title
import java.io.InterruptedIOException
import java.util.*
import jem.format.util.M as T

class Nav(val title: String, val href: String = "", val id: String = "") {
    var parent: Nav? = null

    val items = LinkedList<Nav>()

    val usableHref: String
        get() = if (href.isNotEmpty()) href else items.firstOrNull()?.usableHref ?: ""

    fun newNav(title: String, href: String, id: String = "") {
        items += Nav(title, href, id).also { it.parent = this }
    }
}

internal fun renderText(chapter: Chapter, suffix: String, data: DataHolder) {
    val isSection = chapter.isSection
    if (chapter.isNotRoot) {
        if (isSection) {
            renderSection("section$suffix", chapter, data)
            data.nav = data.nav.items.last
        } else {
            renderChapter("chapter$suffix", chapter, data)
        }
    }
    if (isSection) {
        chapter.forEachIndexed { index, stub ->
            if (Thread.interrupted()) throw InterruptedIOException()
            renderText(stub, "$suffix-${index + 1}", data)
        }
        data.nav.parent?.let { data.nav = it }
    }
}

internal fun renderIntro(intro: Text, title: String, data: DataHolder) = renderPage(title, "intro", true, data) {
    tag("div") {
        attr["class"] = "intro"
        tag("h1", title)
        for (line in intro) {
            tag("p", line.trim())
        }
    }
}

private fun renderSection(id: String, section: Chapter, data: DataHolder) {
    val title = section.title
    val cover = section.cover
    val intro = section.intro

    // no cover, no intro
    if (cover == null && intro?.isEmpty() != false) {
        data.nav.newNav(title, "", id)
        return
    }
    // with cover, no intro
    if (intro?.isEmpty() != false) {
        renderCover(cover!!, "$id-cover", section.title, section.title, data)
        return
    }
    val href = cover?.let { data.writeFlob(it, "$id-cover").href }
    renderPage(title, id, true, data) {
        if (href != null) {
            attr["style"] = "background-image: url(${"../$href"}); background-size: cover;"
            tag("div") {
                attr["style"] = "background: url(${data.maskHref}) repeat;"
                attr["class"] = "section mask"
                for (line in intro) {
                    tag("p", line.trim())
                }
            }
        } else {
            tag("div") {
                attr["class"] = "section"
                tag("h1", title)
                for (line in intro) {
                    tag("p", line.trim())
                }
            }
        }
    }
}

private fun renderChapter(id: String, chapter: Chapter, data: DataHolder) {
    val title = chapter.title
    val cover = chapter.cover
    val intro = chapter.intro
    val text = chapter.text

    if (cover != null) {
        renderCover(cover, "$id-cover", title, title, data)
    }
    renderPage(title, id, true, data) {
        tag("div") {
            attr["class"] = "chapter"
            tag("h1", title)
            intro?.ifNotEmpty { intro ->
                tag("div") {
                    attr["class"] = "brief"
                    for (line in intro) {
                        tag("p", line.trim())
                    }
                    tag("hr")
                }
            }
            if (text != null) {
                tag("div") {
                    attr["class"] = "text"
                    for (line in text) {
                        tag("p", line.trim())
                    }
                }
            }
        }
    }
}

internal fun renderCover(image: Flob, id: String, title: String, altText: String, data: DataHolder) {
    val href = data.writeFlob(image, id).href
    renderPage(title, "$id-page", true, data) {
        tag("div") {
            attr["class"] = "cover"
            tag("img") {
                attr["src"] = "../$href"
                attr["alt"] = altText
            }
        }
    }
}

internal fun renderCover(href: String, id: String, title: String, alt: String, data: DataHolder) = renderPage(title, id, true, data) {
    tag("div") {
        attr["class"] = "cover"
        tag("img") {
            attr["src"] = "../$href"
            attr["alt"] = alt
        }
    }
}

internal inline fun renderPage(title: String, id: String, linear: Boolean, data: DataHolder, block: Tag.() -> Unit): Resource {
    with(data) {
        val opsPath = "$textDir/$id.xhtml"
        writer.xmlDsl("$opsDir/$opsPath", settings) {
            if (epubVersion == 2) {
                doctype("html", "PUBLIC", "-//W3C//DTD XHTML 1.1//EN", "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd")
            } else {
                doctype("html")
            }
            tag("html") {
                attr["xmlns"] = EPUB.XMLNS_XHTML
                if (epubVersion == 3) {
                    attr["xmlns:epub"] = EPUB.XMLNS_OPS
                }
                attr["xml:lang"] = lang
                tag("head") {
                    tag("title", title)
                    tag("meta") {
                        if (epubVersion == 3) {
                            attr["charset"] = encoding
                        } else {
                            attr["http-equiv"] = "Content-Type"
                            attr["content"] = "text/html; charset=$encoding"
                        }
                    }
                    tag("link") {
                        attr["rel"] = "stylesheet"
                        attr["type"] = EPUB.MIME_CSS
                        attr["href"] = cssHref
                    }
                }
                tag("body", block)
            }
        }
        nav.newNav(title, "$id.xhtml", id)
        pkg.spine.addRef(id, linear, if (useDuokanCover && "cover" in id) EPUB.SPINE_DUOKAN_FULLSCREEN else "")
        return pkg.manifest.addResource(id, opsPath, EPUB.MIME_XHTML)
    }
}
