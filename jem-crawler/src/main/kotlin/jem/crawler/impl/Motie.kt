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
import jclp.text.remove
import jclp.text.textOf
import jclp.toLocalDateTime
import jclp.toLocalTime
import jem.*
import jem.crawler.*
import org.jsoup.nodes.Document
import java.time.LocalDate
import java.time.LocalDateTime
import jem.crawler.M as T

class Motie : AbstractCrawler() {
    override val name = T.tr("motie.com")

    override val keys = setOf("www.motie.com")

    override fun getBook(url: String, settings: Settings?): Book {
        val path = url.removeSuffix("#catalog").removeSuffix("#brief")

        val book = Book()
        val extensions = book.extensions
        extensions[EXT_CRAWLER_SOURCE_SITE] = "motie"

        val bookId = baseName(path)
        extensions[EXT_CRAWLER_BOOK_ID] = bookId

        val soup = fetchSoup(path, "get", settings)
        book.genre = soup.selectFirst("div.path a:eq(2)").text()

        val stub = soup.selectFirst("div.work_brief")
        book.cover = CrawlerFlob(stub.selectImage("img")!!, "get", settings)
        book.title = stub.selectFirst("span.name").text()
        book.author = stub.selectFirst("a.author").text()
        book.state = stub.selectFirst("div.tags span.isfinish").text()
        book[KEYWORDS] = stub.selectText("div.tags span[class=fl]", Attributes.VALUE_SEPARATOR)
        book.words = stub.selectFirst("div.hits span").text()

        book.intro = textOf(soup.selectFirst("p.summary1").text())

        extensions[EXT_CRAWLER_LAST_CHAPTER] = soup.selectFirst("div.newchapter span.chaptername a").text()
        val str = soup.selectFirst("div.newchapter span.updatetime").text().remove("更新时间 : ")
        extensions[EXT_CRAWLER_UPDATE_TIME] = parseTime(str)

        getContents(book, soup, settings)
        return book
    }

    override fun getText(url: String, settings: Settings?): String {
        return fetchSoup(url, "get", settings).selectText("div.intro div", System.lineSeparator())
    }

    private fun getContents(book: Book, soup: Document, settings: Settings?) {
        var section: Chapter? = null
        for (div in soup.select("div.catebg > div")) {
            if (div.className() == "cate-tit") {
                section = book.newChapter(div.text())
            } else {
                for (a in div.select("a")) {
                    val chapter = (section ?: book).newChapter(a.selectFirst("span").text())
                    chapter[ATTR_CHAPTER_UPDATE_TIME] = a.attr("data-time")
                    val url = "http://m.motie.com/wechat${a.attr("href")}"
                    chapter.text = CrawlerText(url, chapter, this, settings)
                    chapter[ATTR_CHAPTER_SOURCE_URL] = url
                }
            }
        }
    }

    private fun parseTime(str: String): LocalDateTime {
        val count = str.count { it == '-' }
        return when {
            count == 2 -> "$str:00".toLocalDateTime()
            count == 1 -> "${LocalDate.now().year}-$str:00".toLocalDateTime()
            str.startsWith("昨天") -> LocalDateTime.of(LocalDate.now().minusDays(1), "${str.substring(2)}:00".toLocalTime())
            else -> LocalDateTime.of(LocalDate.now(), "${str.substring(2)}:00".toLocalTime())
        }
    }
}
