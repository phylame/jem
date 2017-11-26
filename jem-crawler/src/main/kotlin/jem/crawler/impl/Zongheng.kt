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
import jclp.toLocalDate
import jem.*
import jem.crawler.*
import org.jsoup.nodes.Element
import jem.crawler.M as T

class Zongheng : AbstractCrawler() {
    override val name = T.tr("zongheng.com")

    override val keys = setOf("book.zongheng.com", "m.zongheng.com")

    override fun getBook(url: String, settings: Settings?): Book {
        val path = url.replace("showchapter", "book")

        val book = Book()
        val extensions = book.extensions
        extensions[EXT_CRAWLER_SOURCE_SITE] = "zongheng"

        val bookId = baseName(path)
        extensions[EXT_CRAWLER_BOOK_ID] = bookId

        val soup = fetchSoup(path, "get", settings)

        val head = soup.head()
        book.title = getMeta(head, "title")
        book.author = getMeta(head, name = "novel:author")
        book.state = getMeta(head, name = "novel:status")
        book.intro = textOf(getMeta(head, "description").replace("(\\s|\u3000)+".toRegex(), System.lineSeparator()))
        book.cover = CrawlerFlob(getMeta(head, "image"), "get", settings)
        extensions[EXT_CRAWLER_UPDATE_TIME] = getMeta(head, name = "novel:update_time").toLocalDate()

        val body = soup.body()
        book.genre = body.attr("categoryName") + (body.attr("childCategoryName")?.let { "/$it" } ?: "")

        val stub = soup.selectFirst("div.status")
        book.words = stub.selectFirst("span").text()
        book[KEYWORDS] = (stub.selectText("div.keyword a", Attributes.VALUE_SEPARATOR))

        extensions[EXT_CRAWLER_LAST_CHAPTER] = soup.selectFirst("a.chap").ownText().remove("正文：")

        getContents(book, bookId, settings)
        return book
    }

    override fun getText(url: String, settings: Settings?): String {
        return fetchSoup(url, "get", settings).selectText("#readerFs p", System.lineSeparator())
    }

    private fun getContents(book: Book, bookId: String, settings: Settings?) {
        val url = "http://book.zongheng.com/showchapter/$bookId.html"
        val soup = fetchSoup(url, "get", settings)
        for (div in soup.select("#chapterListPanel div.booklist")) {
            val section = book.newChapter(div.attr("tomename"))
            for (td in div.select("td.chapterBean")) {
                val chapter = section.newChapter(td.attr("chaptername"))
                chapter.words = td.attr("wordnum")
                chapter[ATTR_CHAPTER_UPDATE_TIME] = td.attr("updatetime")
                val u = "http://book.zongheng.com/chapter/$bookId/${td.attr("chapterid")}.html"
                chapter.text = CrawlerText(u, chapter, this, settings)
                chapter[ATTR_CHAPTER_SOURCE_URL] = u
            }
        }
    }

    private fun getMeta(head: Element, property: String = "", name: String = ""): String = if (property.isNotEmpty()) {
        head.selectFirst("meta[property=og:$property]").attr("content")
    } else {
        head.selectFirst("meta[name=og:$name]").attr("content")
    }
}
