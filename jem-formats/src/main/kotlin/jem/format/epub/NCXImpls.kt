package jem.format.epub

import jclp.depth
import jclp.setting.Settings
import jclp.vdm.VDMWriter
import jclp.vdm.useStream
import jem.*
import jem.format.util.XmlRender
import java.util.*

private const val NCX_VERSION = "2005-1"
private const val DTD_ID = "-//NISO//DTD ncx 2005-1//EN"
private const val DTD_URI = "http://www.daisy.org/z3986/2005/ncx-2005-1.dtd"
private const val NCX_XMLNS = "http://www.daisy.org/z3986/2005/ncx/"
private const val NCX_META_PREFIX = "jem-ncx-meta-"

internal class NCXBuilder(
        book: Book, writer: VDMWriter, settings: Settings?, private val opf: OPFBuilder
) : BuilderBase(book, writer, settings) {
    var count = 0
        private set

    private var navPoint = NavPoint("", 0, "", "")

    fun make() {
        opf.tocId = "ncx"
        opf.newItem("ncx", "toc.ncx", MIME_NCX) {
            writer.useStream(it.vdmPath) {
                XmlRender(settings).apply {
                    output(it)
                    renderNCX()
                }
            }
        }
    }

    fun newNav(id: String, title: String, href: String) {
        val nav = NavPoint(id, ++count, title, href)
        navPoint += nav
        navPoint = nav
    }

    fun endNav() {
        navPoint = navPoint.parent
    }

    private fun XmlRender.renderNCX() {
        beginXml()
        docdecl("ncx", DTD_ID, DTD_URI)
        beginTag("ncx")
        attribute("version", NCX_VERSION)
        xmlns(NCX_XMLNS)
        attribute("xml:lang", opf.lang)

        renderMetadata()

        beginTag("docTitle")
        beginTag("text")
        text(book.title)
        endTag()
        endTag()

        for (author in book.author.split(Attributes.VALUE_SEPARATOR)) {
            beginTag("docAuthor")
            beginTag("text")
            text(author)
            endTag()
            endTag()
        }

        beginTag("navMap")
        for (nav in navPoint.children) {
            renderNav(nav)
        }
        endTag()

        endTag()
        endXml()
    }

    private fun XmlRender.renderMetadata() {
        newMeta("dtb:uid", uuid)
        newMeta("dtb:generator", "jem(${Build.VENDOR})")
        newMeta("dtb:depth", book.depth.toString())
        newMeta("dtb:totalPageCount", "0")
        newMeta("dtb:maxPageNumber", "0")

        book.extensions.filter { it.first.startsWith(NCX_META_PREFIX) }.forEach { (name, value) ->
            newMeta(name.substring(NCX_META_PREFIX.length), value.toString())
        }

        beginTag("head")
        for (meta in meta.values) {
            renderMeta(meta.name, meta.content)
        }
        endTag()
    }

    private fun XmlRender.renderMeta(name: String, content: String) {
        beginTag("meta")
        attribute("name", name)
        attribute("content", content)
        endTag()
    }

    private fun XmlRender.renderNav(nav: NavPoint) {
        // section without html page
        val href = nav.href.takeIf(String::isNotEmpty) ?: nav.children.firstOrNull()?.href ?: return

        beginTag("navPoint")
        attribute("id", nav.id)
        attribute("class", if (nav.children.isEmpty()) "chapter" else "section")
        attribute("playOrder", nav.order.toString())

        beginTag("navLabel")
        beginTag("text")
                .text(nav.title)
                .endTag()
        endTag()

        beginTag("content")
        attribute("src", href)
        endTag()

        for (stub in nav.children) {
            renderNav(stub)
        }

        endTag()
    }

    private data class NavPoint(val id: String, val order: Int, val title: String, val href: String) {
        lateinit var parent: NavPoint

        val children = LinkedList<NavPoint>()

        operator fun plusAssign(nav: NavPoint) {
            children += nav
            nav.parent = this
        }
    }
}
