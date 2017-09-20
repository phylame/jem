package jem.format.epub

import jclp.depth
import jclp.flob.Flob
import jclp.setting.Settings
import jclp.setting.getInt
import jem.Book
import jem.author
import jem.cover
import jem.epm.MakerException
import jem.format.util.M
import jem.format.util.XmlRender
import jem.title

internal fun renderNCX(version: String, param: EpubParam, data: OpfData) = when (version) {
    "2005", "2005-1" -> render2005(param, data)
    else -> throw MakerException(M.tr("err.ncx.unsupported", version))
}

private const val DTD_ID = "-//NISO//DTD ncx 2005-1//EN"
private const val DTD_URI = "http://www.daisy.org/z3986/2005/ncx-2005-1.dtd"
private const val NCX_XMLNS = "http://www.daisy.org/z3986/2005/ncx/"

private const val NCX_META_PREFIX = "jem-ncx-meta-"

private fun render2005(param: EpubParam, data: OpfData): NavListener = with(param.render) {
    val book = param.book
    val settings = param.settings

    beginXml()
    docdecl("ncx", DTD_ID, DTD_URI)
    beginTag("ncx")
    attribute("version", "2005-1")
    xmlns(NCX_XMLNS)
    if (data.lang.isNotEmpty()) {
        attribute("xml:lang", data.lang)
    }

    renderHead(book, data, settings)
    renderTitle(book, param, data)
    renderAuthors(book, param, data)

    beginTag("navMap")
    NCXBuilder(this)
}

private fun XmlRender.renderHead(book: Book, data: OpfData, settings: Settings?) {
    beginTag("head")

    val extensions = book.extensions

    renderMeta("dtb:uid", data.bookId)
    renderMeta("dtb:depth", book.depth.toString())
    (settings?.getInt("maker.epub.totalPageCount") ?: extensions["${NCX_META_PREFIX}totalPageCount"] as? Int ?: 0).let {
        renderMeta("dtb:totalPageCount", it.toString())
    }
    (settings?.getInt("maker.epub.maxPageNumber") ?: extensions["${NCX_META_PREFIX}maxPageNumber"] as? Int ?: 0).let {
        renderMeta("dtb:maxPageNumber", it.toString())
    }

    val meta = HashMap<String, String>()
    for ((first, second) in extensions) {
        if (first.startsWith(NCX_META_PREFIX)) {
            meta[first.substring(NCX_META_PREFIX.length)] = second.toString()
        }
    }
    (settings?.get("maker.epub.ncxMeta") as? Map<*, *>)?.let {
        for ((key, value) in it) {
            meta[key.toString()] = value.toString()
        }
    }
    for ((key, value) in meta) {
        renderMeta(key, value)
    }

    endTag()
}

private fun XmlRender.renderTitle(book: Book, param: EpubParam, data: OpfData) {
    beginTag("docTitle")
    renderText(book.title.takeIf(String::isNotEmpty) ?: throw MakerException(M.tr("epub.make.noTitle")))
    book.cover?.let { renderImg(it, COVER_ID, param, data) }
    endTag()
}

private fun XmlRender.renderAuthors(book: Book, param: EpubParam, data: OpfData) {
    book.author.split(";").takeIf { it.isNotEmpty() }?.let {
        it.forEachIndexed { i, author ->
            beginTag("docAuthor")
            renderText(author)
            (book.extensions["jem-author-image-${i + 1}"] as Flob?)?.let {
                renderImg(it, "author-image-${i + 1}", param, data)
            }
            endTag()
        }
    }
}

private fun XmlRender.renderMeta(name: String, content: String) {
    beginTag("meta")
    attribute("name", name)
    attribute("content", content)
    endTag()
}

private fun XmlRender.renderText(text: String) {
    beginTag("text")
    text(text)
    endTag()
}

private fun XmlRender.renderImg(flob: Flob, id: String, param: EpubParam, data: OpfData) {
    beginTag("img")
    attribute("src", data.write(flob, id, param.writer))
    endTag()
}

private class NCXBuilder(val render: XmlRender) : NavListener {
    override fun newNav(id: String, type: String, title: String, cover: String) {
        with(render) {
            beginTag("navPoint")
            attribute("id", id)
            attribute("class", type)

            beginTag("navLabel")
            renderText(title)
            endTag()

            if (cover.isNotEmpty()) {
                beginTag("img")
                attribute("src", cover)
                endTag()
            }
        }
    }

    override fun endNav() {
        render.endTag() // navPoint
    }

    override fun endToc() {
        render.endTag() // navMap

        render.endTag() // ncx
        render.endXml()
    }
}
