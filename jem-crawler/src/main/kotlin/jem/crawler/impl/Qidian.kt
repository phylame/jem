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

import jclp.setting.Settings
import jclp.text.remove
import jclp.text.textOf
import jem.*
import jem.crawler.*
import org.jsoup.nodes.Document
import jem.crawler.M as T

class Qidian : AbstractCrawler() {
    override val name = T.tr("qidian.com");

    override val keys = setOf("book.qidian.com", "m.qidian.com")

    override fun getBook(url: String, settings: Settings?): Book {
        val path = if ("m.qidian.com" in url) {
            url.replace("m.qidian.com/book", "book.qidian.com/info")
        } else {
            url.replace("#Catalog", "")
        }
        val book = Book()
        val soup = fetchSoup(path, "get", settings)

        var stub = soup.select("div.book-information")

        stub.selectImage("img")?.replace("180", "600")?.let { src ->
            book.cover = CrawlerFlob(src, "get", settings, "cover.jpg", "image/jpeg")
        }
        stub = stub.select("div.book-info")
        book.title = stub.select("h1 em").text()
        book.author = stub.select("h1 a").text()
        book.state = stub.select("p.tag span:eq(0)").text()
        book.genre = stub.selectText("p.tag a", "/")
        book["brief"] = stub.select("p.intro").text()
        book.words = stub.select("p em:eq(0), p cite:eq(1)").text().remove(" ")
        book.intro = textOf(soup.selectText("div.book-intro p", System.lineSeparator()))
        book[KEYWORDS] = soup.selectText("p.tag-wrap a", Attributes.VALUE_SEPARATOR)
        book[ATTR_LAST_UPDATE] = (soup.select("li.update a").text())
        getContents(book, soup, settings)
        return book
    }

    private fun getContents(book: Book, soup: Document, settings: Settings?) {
        val toc = soup.select("div.volume-wrap div.volume")
        if (toc.isEmpty()) {
//            getMobileContents(book, soup.baseUri().substring(soup.baseUri().lastIndexOf("/")), settings)
            return
        }
        for (div in toc) {
            val section = book.newChapter(div.selectFirst("h3").subText(0))
            section.words = div.selectText("h3 cite")
            for (a in div.select("li a")) {
                val chapter = section.newChapter(a.text())
                a.attr("title").let {
                    chapter["updateTime"] = (it.substring(5, 24))
                    chapter.words = it.substring(30)
                }
                val url = a.absUrl("href")
                chapter.text = CrawlerText(url, chapter, this, settings)
                chapter[ATTR_SOURCE_URL] = url
            }
        }
    }

    private fun getMobileContents(book: Book, bookId: String, settings: Settings?) {

    }

    override fun getText(url: String, settings: Settings?) = fetchSoup(url, "get", settings).let {
        if ("m.qidian.com" in url) {
            it.selectText("article#chapterContent p", System.lineSeparator())
        } else {
            it.selectText("div.read-content p", System.lineSeparator())
        }
    }
}
