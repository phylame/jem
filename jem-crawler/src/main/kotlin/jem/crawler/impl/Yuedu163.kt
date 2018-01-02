package jem.crawler.impl

import jclp.io.baseName
import jclp.io.dirName
import jclp.setting.Settings
import jclp.text.ifNotEmpty
import jclp.text.textOf
import jclp.toLocalDateTime
import jclp.toLocalTime
import jem.*
import jem.crawler.*
import org.json.JSONObject
import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import jem.crawler.M as T

class Yuedu163 : ReusableCrawler() {
    override val name = T.tr("yuedu.163.com")

    override val keys = setOf("guofeng.yuedu.163.com", "caiwei.yuedu.163.com", "m.yuedu.163.com")

    override fun getBook(url: String, settings: Settings?): Book =
            if ("m.yuedu" in url) getMobileBook(url, settings) else getPcBook(url, settings)

    private fun getMobileBook(url: String, settings: Settings?): Book {
        val path = if ("reader" in url) {
            dirName(url).replaceFirst("reader/book", "source").removeSuffix("/")
        } else {
            url
        }
        val book = CrawlerBook(path, "yuedu163")
        val soup = fetchSoup(path, "get", settings)
        val stub = soup.selectFirst("section.m-book-info")
        book.cover = CrawlerFlob(stub.selectImage("img")!!, "get", settings, "cover.jpg")
        book.title = stub.selectFirst("h1").text()
        book.author = stub.select("p:eq(2)").text().substring(3)
        stub.select("p:eq(3)").text().substring(3).split("|").let {
            book.genre = it.first().trim()
            book.state = it.last().trim()
        }
        book.words = stub.select("p:eq(4)").text().substring(3)
        soup.selectFirst("section.m-book-recent").let {
            book.lastChapter = it.selectFirst("a").text()
            book.updateTime = parseTime(it.selectFirst("p").text().removeSuffix("更新"))
        }
        book.intro = textOf(soup.selectText("section.m-book-detail p", System.lineSeparator()))
        getMobileContents(book, soup.baseUri(), settings)
        return book
    }

    private fun getMobileContents(book: Book, baseUrl: String, settings: Settings?) {
        val url = baseUrl.replaceFirst("source/", "reader/book/info.json?source_uuid=")
        val json = fetchJson(url, "get", settings)
        var section: Chapter? = null
        for (item in json.getJSONObject("data").getJSONArray("catalog")) {
            val spec = item as JSONObject
            if (spec.getInt("grade") == 2) {
                val chapter = (section ?: book).newChapter(spec.getString("title"))
                chapter.words = spec.getInt("wordCount").toString()
                chapter.setText("${url.replaceFirst("info", "content")}&content_uuid=${spec.getString("secId")}", settings)
            } else {
                section = book.newChapter(spec.getString("title"))
            }
        }
    }

    private fun getPcBook(url: String, settings: Settings?): Book {
        val path = when {
            "book_reader" in url -> url.replaceFirst("book_reader", "source")
            "newBookReader" in url -> url.replaceFirst("newBookReader.do?operation=catalog&sourceUuid=", "source/")
            else -> url
        }
        val book = CrawlerBook(path, "yuedu163")
        book.bookId = baseName(path)
        val soup = fetchSoup(path, "get", settings)
        var stub = soup.selectFirst("div.g-sd")
        book.state = stub.selectFirst("span.status").text()
        book.genre = stub.selectFirst("a").text()
        book.words = stub.selectFirst("tr:eq(1) td:eq(1)").text()
        book[KEYWORDS] = stub.selectText("div.m-tags a", Attributes.VALUE_SEPARATOR)
        stub = soup.selectFirst("div.g-mn")
        book.title = stub.selectFirst("h3").attr("title")
        book.author = stub.selectFirst("h3 a").text()
        book.cover = CrawlerFlob(stub.selectImage("img")!!, "get", settings, "cover.jpg")
        book.intro = textOf(stub.selectText("div.description", System.lineSeparator()))

        book.lastChapter = stub.selectFirst("h4").text()
        book.updateTime = parseTime(stub.selectFirst("span.updatetime").text().substring(5))

        getPcContents(book, soup.baseUri(), settings)
        return book
    }

    private fun getPcContents(book: Book, baseUrl: String, settings: Settings?) {
        val url = "${baseUrl.replaceFirst("source/", "newBookReader.do?operation=info&sourceUuid=")}&catalogOnly=true"
        val json = fetchJson(url, "get", settings)
        var section: Chapter? = null
        for (item in json.getJSONArray("catalog")) {
            val spec = item as JSONObject
            if (spec.getInt("grade") == 2) {
                val chapter = (section ?: book).newChapter(spec.getString("title"))
                chapter.words = spec.getInt("wordCount").toString()
                chapter.setText("${baseUrl.replaceFirst("source/", "getArticleContent.do?sourceUuid=")}&articleUuid=${spec.getString("secId")}", settings)
            } else {
                section = book.newChapter(spec.getString("title"))
            }
        }
    }

    private fun parseTime(str: String): LocalDateTime {
        return when {
            str.startsWith("今天") -> LocalDate.now().atTime("${str.substring(3)}:00".toLocalTime())
            str.startsWith("昨天") -> LocalDate.now().minusDays(1).atTime("${str.substring(3)}:00".toLocalTime())
            "分钟" in str -> LocalDate.now().atTime(LocalTime.now().minusSeconds(str.removeSuffix("分钟前").toLong()))
            else -> "$str:00".toLocalDateTime()
        }
    }

    override fun getText(url: String, settings: Settings?): String {
        if ("m.yuedu" in url) {
            return fetchJson(url, "get", settings).getJSONObject("data").getString("content").ifNotEmpty {
                Jsoup.parse(Base64.getDecoder().decode(it).toString(Charsets.UTF_8)).selectText("p", System.lineSeparator())
            } ?: ""
        } else {
            return fetchJson(url, "get", settings).getString("content").ifNotEmpty {
                Jsoup.parse(Base64.getDecoder().decode(it).toString(Charsets.UTF_8)).selectText("p", System.lineSeparator())
            } ?: ""
        }
    }
}
