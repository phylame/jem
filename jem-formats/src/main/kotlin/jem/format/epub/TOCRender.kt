package jem.format.epub

import jclp.setting.Settings
import jclp.vdm.VDMWriter
import jem.Book
import jem.format.util.XmlRender
import java.util.*

interface NavListener {
    fun newNav(id: String, type: String, title: String, cover: String)

    fun endNav()
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
    }

    private fun renderCover(data: Local) {}

    private fun renderIntro(data: Local) {}

    private fun renderSection(data: Local) {}

    private fun renderChapter(data: Local) {}

    private data class Local(val book: Book, val writer: VDMWriter, val settings: Settings?, val data: OpfData) {
        val render = XmlRender(settings)

        fun useXHTML(title: String, block: XmlRender.() -> Unit) = with(render) {
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