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
import jclp.text.textOf
import jem.*
import jem.crawler.*
import org.jsoup.nodes.Document
import jem.crawler.M as T

class Qidian : AbstractCrawler() {
    override val name = T.tr("qidian.com");

    override val keys = setOf("book.qidian.com", "m.qidian.com")

    override fun getBook(url: String, settings: Settings?): Book {
        val path = if (url.contains("m.qidian.com")) {
            url.replace("m.qidian.com/book", "book.qidian.com/info");
        } else {
            url.replace("#Catalog", "");
        }
        val book = CrawlerBook()
        val soup = fetchSoup(path, "get", settings)
        val stub = soup.select("div.book-information div.book-info")
        book.title = stub.selectText("h1 em")
        soup.selectImage("a#bookImg img")?.let {
            book.cover = CrawlerFlob(it.replaceFirst("[\\d]+$".toRegex(), "600") + ".jpg", "get", settings)
        }
        book.author = stub.selectText("h1 a")
        book.state = stub.selectText("p.tag span:eq(0)")
        book.genre = stub.selectText("p.tag a", "/")
        book["brief"] = soup.selectText("p.intro")
        book.words = stub.selectText("p em:eq(0)") + stub.selectText("p cite:eq(1)")
        book.intro = textOf(soup.selectText("div.book-intro p", System.lineSeparator()))
        getContents(book, soup, settings)
        return book
    }

    private fun getContents(book: Book, soup: Document, settings: Settings?) {
        val toc = soup.select("div.volume-wrap div.volume")
        if (toc.isEmpty()) {
            getMobileContents(book, soup.baseUri().substring(soup.baseUri().lastIndexOf("/")), settings)
            return
        }
        for (div in toc) {
            val section = book.newChapter(div.select("h3").first().subText(0))
            section.words = div.selectText("h3 cite")
            for (a in div.select("li a")) {
                val url = a.absUrl("href")
//                val chapter = section.newChapter(trimmed(a.text()))
//                chapter.setText(CrawlerText(url, this, config, chapter))
//                setValue(chapter, "source", url)
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
