/*
 * Copyright 2015-2017 Peng Wan <phylame@163.com>
 *
 * This file is part of Jem.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jem.crawler.impl

import jclp.io.baseName
import jclp.setting.Settings
import jclp.text.ifNotEmpty
import jclp.text.remove
import jclp.text.textOf
import jclp.toLocalDateTime
import jclp.toLocalTime
import jem.*
import jem.crawler.*
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.nodes.Document
import java.time.LocalDate
import java.time.LocalDateTime
import jem.crawler.M as T

class Qidian : ReusableCrawler() {
    override val name = T.tr("qidian.com")

    override val keys = setOf("book.qidian.com", "m.qidian.com")

    override fun getBook(url: String, settings: Settings?): Book {
        val path = if ("m.qidian.com" in url) {
            url.replace("m.qidian.com/book", "book.qidian.com/info")
        } else {
            url.remove("#Catalog")
        }
        val book = CrawlerBook(path, "qidian")
        val bookId = baseName(url).apply { book.bookId = this }

        val soup = fetchSoup(path, "get", settings)

        var stub = soup.selectFirst("div.book-information")
        stub.selectImage("img")?.replace("180", "600")?.let { src ->
            book.cover = CrawlerFlob(src, "get", settings, "cover.jpg", "image/jpeg")
        }
        stub = stub.selectFirst("div.book-info")
        book.title = stub.selectFirst("h1 em").text()
        book.author = stub.selectFirst("h1 a").text()
        book.state = stub.selectFirst("p.tag span:eq(0)").text()
        book.genre = stub.selectText("p.tag a", "/")
        book["brief"] = stub.selectFirst("p.intro").text()
        book.words = stub.select("p em:eq(0), p cite:eq(1)").text().remove(" ")
        book.intro = textOf(soup.selectText("div.book-intro p", System.lineSeparator()))
        soup.selectText("p.tag-wrap a", Attributes.VALUE_SEPARATOR).ifNotEmpty {
            book[jem.KEYWORDS] = it
        }

        book.lastChapter = soup.selectFirst("li.update a").text()
        book.updateTime = parseTime(soup.selectFirst("li.update em").text())

        getContents(book, soup, bookId, settings)
        return book
    }

    private fun getContents(book: Book, soup: Document, bookId: String, settings: Settings?) {
        val toc = soup.select("div.volume-wrap div.volume")
        if (toc.isEmpty()) {
            getMobileContents(book, bookId, settings)
            return
        }
        for (div in toc) {
            val section = book.newChapter(div.selectFirst("h3").subText(0))
            section.words = div.selectText("h3 cite")
            for (a in div.select("li a")) {
                val chapter = section.newChapter(a.text())
                a.attr("title").let {
                    chapter[ATTR_CHAPTER_UPDATE_TIME] = (it.substring(5, 24))
                    chapter.words = it.substring(30)
                }
                chapter.setText(a.absUrl("href"), settings)
            }
        }
    }

    private fun getMobileContents(book: Book, bookId: String, settings: Settings?) {
        val baseUrl = "https://m.qidian.com/book/$bookId"
        val data = fetchString("$baseUrl/catalog", "get", settings)
        val start = data.indexOf("[{", data.indexOf("g_data.volumes"))
        for (obj in JSONArray(data.substring(start, data.lastIndexOf("}];") + 2))) {
            if (obj !is JSONObject) continue
            val section = book.newChapter(obj.getString("vN"))
            for (ch in obj.getJSONArray("cs")) {
                if (ch !is JSONObject) continue
                val chapter = section.newChapter(ch.getString("cN"))
                chapter[ATTR_CHAPTER_UPDATE_TIME] = ch.getString("uT")
                chapter[WORDS] = ch.getInt("cnt").toString()
                chapter.setText("$baseUrl/${ch.getLong("id")}", settings)
            }
        }
    }

    private fun parseTime(str: String): LocalDateTime = when {
        '-' in str -> str.toLocalDateTime()
        str.startsWith("今天") -> LocalDateTime.of(LocalDate.now(), "${str.substring(2, 7)}:00".toLocalTime())
        "小时" in str -> LocalDateTime.now().minusHours(str.removeSuffix("小时前").toLong())
        else -> LocalDateTime.of(LocalDate.now().minusDays(1), "${str.substring(2, 7)}:00".toLocalTime())
    }

    override fun getText(url: String, settings: Settings?) = fetchSoup(url, "get", settings).let {
        if ("m.qidian.com" in url) {
            it.selectText("article#chapterContent p", System.lineSeparator())
        } else {
            it.selectText("div.read-content p", System.lineSeparator())
        }
    }
}
