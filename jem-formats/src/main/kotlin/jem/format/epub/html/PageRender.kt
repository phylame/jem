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
import jem.format.util.xmlDsl
import jem.intro
import jem.title
import java.io.InterruptedIOException
import jem.format.util.M as T

private fun renderText(chapter: Chapter, suffix: String, data: DataHolder) {
    val isSection = chapter.isSection
    if (chapter.isNotRoot) {
        val id = if (isSection) "section$suffix" else "chapter$suffix"
        if (isSection) {
            renderSection(id, chapter, data)
//            data.nav = data.nav.items.last
        } else {
            renderChapter(id, chapter, data)
        }
    }
    if (isSection) {
        chapter.forEachIndexed { index, stub ->
            if (Thread.interrupted()) throw InterruptedIOException()
            renderText(stub, "$suffix-${index + 1}", data)
        }
//        data.nav.parent?.let { data.nav = it }
    }
}

private fun renderCover(cover: Flob, data: DataHolder) {
    renderImage(cover, "cover", T.tr("epub.make.coverTitle"), data.book.title, true, data)
}

private fun renderIntro(intro: Text, data: DataHolder) {
    val title = T.tr("epub.make.introTitle")
    renderPage(title, "intro", true, data) {
        tag("div") {
            attr["class"] = "main intro"
            tag("h1", title)
            intro.forEach { tag("p", it.trim()) }
        }
    }
}

private fun renderSection(id: String, section: Chapter, data: DataHolder) {
    val cover = section.cover
    val intro = section.intro
    val title = section.title

    if (cover == null && intro?.isEmpty() != false) {
//        data.nav.newNav(title, "")
        return
    }

    if (intro?.isEmpty() != false) { // with cover, no intro
        renderImage(cover!!, "$id-cover", section.title, section.title, true, data)
        return
    }

    val coverHref = cover?.let {
        data.writeFlob(it, "$id-cover")
    }

    renderPage(title, id, true, data) {
        if (coverHref != null) {
            attr["style"] = "background-image: url(${"../$coverHref"});background-size: cover;"
            tag("div") {
                intro.ifNotEmpty {
//                    attr["style"] = "background: url(${data.maskHref}) repeat;"
                    attr["class"] = "main section"
                    intro.forEach { tag("p", it.trim()) }
                }
            }
        } else {
            intro.ifNotEmpty {
                tag("div") {
                    attr["class"] = "main section"
                    tag("h1", title)
                    it.forEach { tag("p", it.trim()) }
                }
            }
        }
    }
}

private fun renderChapter(id: String, chapter: Chapter, data: DataHolder) {
    val cover = chapter.cover
    val intro = chapter.intro
    val text = chapter.text
    val title = chapter.title

    if (cover != null) {
        renderImage(cover, "$id-cover", title, title, false, data)
    }

    if (intro?.isEmpty() != false && text?.isEmpty() != false) {
//        data.nav.newNav(title, "")
        return
    }

    renderPage(title, id, true, data) {
        tag("div") {
            attr["class"] = "main chapter"
            tag("h1", title)
            intro?.ifNotEmpty {
                tag("section") {
                    attr["class"] = "brief"
                    it.forEach { tag("p", it.trim()) }
                    tag("hr")
                }
            }
            if (text != null) {
                tag("section") {
                    attr["class"] = "text"
                    text.forEach { tag("p", it.trim()) }
                }
            }
        }
    }
}

private fun renderImage(image: Flob, id: String, title: String, altText: String, addToNav: Boolean, data: DataHolder) {
    val coverHref = data.writeFlob(image, id).also {
        if (id == "cover") {
            it.attr["properties"] = EPUB.MANIFEST_COVER_IMAGE
        }
    }
    renderPage(title, "$id-page", addToNav, data) {
        tag("div") {
            attr["class"] = "main cover"
            tag("img") {
                attr["src"] = "../$coverHref"
                attr["alt"] = altText
            }
        }
    }
}

private inline fun renderPage(title: String, id: String, addToNav: Boolean, data: DataHolder, block: Tag.() -> Unit) {
    with(data) {
        val opsPath = "$textDir/$id.xhtml"
        writer.xmlDsl("$opsDir/$opsPath", settings) {
            doctype("html")
            tag("html") {
                attr["xmlns"] = EPUB.XMLNS_XHTML
                attr["xmlns:epub"] = EPUB.XMLNS_OPS
                attr["lang"] = lang
                tag("head") {
                    tag("meta") {
                        attr["charset"] = encoding
                    }
                    tag("title", title)
                    tag("link") {
                        attr["rel"] = "stylesheet"
                        attr["type"] = EPUB.MIME_CSS
//                        attr["href"] = cssHref
                    }
                }
                tag("body", block)
            }
        }
//        if (addToNav) nav.newNav(title, "$id.xhtml")
//        pkg.manifest.addResource(id, opsPath, EPUB.MIME_XHTML, if (id == "nav") EPUB.MANIFEST_NAVIGATION else "")
//        if (id != "nav") { // nav is already added
//            pkg.spine.addReference(id, properties = if (useDuokanCover && "cover" in id) EPUB.SPINE_DUOKAN_FULLSCREEN else "")
//        }
    }
}
