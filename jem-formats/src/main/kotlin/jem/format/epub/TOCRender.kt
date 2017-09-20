package jem.format.epub

import jclp.flob.Flob
import jclp.setting.Settings
import jclp.text.Text
import jclp.vdm.VDMWriter
import jclp.vdm.useStream
import jem.Book
import jem.cover
import jem.format.util.M
import jem.format.util.XmlRender
import jem.intro
import java.util.*

interface NavListener {
    fun newNav(id: String, type: String, title: String, cover: String)

    fun endNav()

    fun endToc()
}

interface TOCRender {
    fun addNavListener(listener: NavListener)

    fun render(book: Book, writer: VDMWriter, settings: Settings?, data: OpfData)
}

object DefaultTOCRender : TOCRender {
    private val listeners = LinkedList<NavListener>()

    override fun addNavListener(listener: NavListener) {
        listeners += listener
    }

    override fun render(book: Book, writer: VDMWriter, settings: Settings?, data: OpfData) {
        val local = Local(book, writer, settings, data)
        book.cover?.let { renderCover(it, local) }
        book.intro?.let { renderIntro(it, local) }
    }

    private fun renderCover(cover: Flob, local: Local) {
        val href = local.data.write(cover, COVER_ID, local.writer)
        local.writer.useStream("cover.xhtml") { stream ->
            local.render.output(stream)
            local.useXHTML(M.tr("epub.make.coverTitle")) {
                beginTag("img")
                attribute("class", "cover")
                attribute("src", "")
                endTag()
            }
        }
    }

    private fun renderIntro(intro: Text, local: Local) {

    }

    private fun renderSection(data: Local) {}

    private fun renderChapter(data: Local) {}

    private data class Local(val book: Book, val writer: VDMWriter, val settings: Settings?, val data: OpfData) {
        val render = XmlRender(settings)

        inline fun useXHTML(title: String, block: XmlRender.() -> Unit) = with(render) {
            beginXml()
            docdecl("html", "-//W3C//DTD XHTML 1.1//EN", "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd")
            beginTag("html")
            xmlns("http://www.w3.org/1999/xhtml")
            attribute("xmlns:xml", "http://www.w3.org/XML/1998/namespace")
            attribute("xml:lang", data.lang)

            beginTag("head")

            beginTag("title")
                    .text(title)
                    .endTag()

            beginTag("link")
                    .attribute("href", "")
                    .attribute("rel", "stylesheet")
                    .attribute("type", "text/css")
                    .endTag()

            endTag() // head

            beginTag("body")
            block()
            endTag()

            endTag()
            endXml()
        }
    }
}
