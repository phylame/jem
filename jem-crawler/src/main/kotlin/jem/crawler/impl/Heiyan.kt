package jem.crawler.impl

import jclp.io.baseName
import jclp.setting.Settings
import jclp.text.count
import jclp.text.textOf
import jclp.toLocalDateTime
import jem.*
import jem.crawler.*
import org.jsoup.nodes.Element
import java.time.LocalDate
import jem.crawler.M as T

class Heiyan : AbstractCrawler() {
    override val name = T.tr("heiyan.com")

    override val keys = setOf("www.heiyan.com")

    override fun getBook(url: String, settings: Settings?): Book {
        val path = url.removeSuffix("/chapter")

        val book = Book()
        val extensions = book.extensions
        extensions[EXT_CRAWLER_SOURCE_URL] = path
        extensions[EXT_CRAWLER_SOURCE_SITE] = "heiyan"

        val bookId = baseName(path)
        extensions[EXT_CRAWLER_BOOK_ID] = bookId

        val soup = fetchSoup(path, "get", settings)

        val head = soup.head()
        book.title = getMeta(head, "title")
        book.author = getMeta(head, "novel:author")
        book.state = getMeta(head, "novel:status")
        book.genre = getMeta(head, "novel:category")
        book.cover = CrawlerFlob(getMeta(head, "image").let { it.substring(0, it.indexOf('@')) }, "get", settings)
        extensions[EXT_CRAWLER_UPDATE_TIME] = getMeta(head, "novel:update_time").let {
            (if (it.count('-') == 2) "$it:00" else "${LocalDate.now().year}-$it:00").toLocalDateTime()
        }
        extensions[EXT_CRAWLER_LAST_CHAPTER] = getMeta(head, "novel:latest_chapter_name")

        val stub = soup.selectFirst("div.pattern-cover-detail")
        book.words = stub.selectFirst("span.words").text().removeSuffix("字")
        book.intro = textOf(stub.selectFirst("pre").text().trim().replace("(\\s|\u3000)+".toRegex(), System.lineSeparator()))

        getContents(book, "$path/chapter", settings)
        return book
    }

    override fun getText(url: String, settings: Settings?): String {
        return fetchSoup(url, "get", settings).selectText("div.page-content p", System.lineSeparator())
    }

    protected fun getContents(book: Book, url: String, settings: Settings?) {
        val soup = fetchSoup(url, "get", settings)
        var section: Chapter? = null
        for (div in soup.select("div.chapter-list > div")) {
            if (div.className() == "hd") {
                section = book.newChapter(div.text())
            } else {
                for (li in div.select("li")) {
                    val a = li.selectFirst("a")
                    val chapter = (section ?: book).newChapter(a.text())
                    chapter[ATTR_CHAPTER_UPDATE_TIME] = li.attr("createdate")
                    val u = a.absUrl("href").replace("www", "w")
                    chapter.text = CrawlerText(u, chapter, this, settings)
                    chapter[ATTR_CHAPTER_SOURCE_URL] = u
                }
            }
        }
    }

    private fun getMeta(head: Element, name: String): String {
        return head.selectFirst("meta[property=og:$name]").attr("content")
    }
}
