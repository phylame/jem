package jem.format.epub

import jclp.flob.Flob
import jclp.io.getProperties
import jclp.setting.Settings
import jclp.text.Text
import jclp.vdm.VDMWriter
import jem.Book
import jem.cover
import jem.format.util.M
import jem.format.util.XmlRender
import jem.intro
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import java.nio.file.Paths
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

    init {
        Velocity.init(getProperties("!jem/format/epub/velocity.properties"))
    }

    override fun addNavListener(listener: NavListener) {
        listeners += listener
    }

    override fun render(book: Book, writer: VDMWriter, settings: Settings?, data: OpfData) {
        val local = Local(book, writer, settings, data)
        book.cover?.let { renderCover(it, local) }
        book.intro?.let { renderIntro(it, local) }
    }

    private fun renderCover(cover: Flob, local: Local) {
        val href = local.data.write(cover, COVER_ID, "./images/", local.writer)
        local.context.put("coverHref", href)
//        local.data.stream("./text/cover.xhtml", local.writer, id = "cover-page") {
//            it.writer().use {
//                getTemplate("cover").merge(local.context, it)
//            }
//        }
    }

    private fun renderIntro(intro: Text, local: Local) {

    }

    private fun renderSection(data: Local) {}

    private fun renderChapter(data: Local) {}

    private data class Local(val book: Book, val writer: VDMWriter, val settings: Settings?, val data: OpfData) {
        val root = Paths.get("OEBPS")

        val render = XmlRender(settings)

        val context = VelocityContext()

        init {
            context.put("M", M)
            context.put("book", book)
        }
    }

    private fun getTemplate(name: String) = Velocity.getTemplate("jem/format/epub/$name.vm")

}
