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
import jclp.text.textOf
import jclp.toLocalDateTime
import jem.*
import jem.crawler.*
import jem.crawler.M as T

class Duxia : ReusableCrawler() {
    override val name = T.tr("duxia.org")

    override val keys = setOf("www.duxia.org")

    override fun getBook(url: String, settings: Settings?): Book {
        val path = url.replace("du/[\\d]+/([\\d]+)/".toRegex(), "book/$1.html")

        val book = CrawlerBook(path, "duxia")
        book.bookId = baseName(path)

        val soup = fetchSoup(path, "get", settings)

        val head = soup.head()
        book.title = getOgMeta(head, "title")
        book.author = getOgMeta(head, "novel:author")
        book.genre = getOgMeta(head, "novel:category")
        book.cover = CrawlerFlob(getOgMeta(head, "image"), "get", settings)
        book.state = getOgMeta(head, "novel:status")
        book.updateTime = getOgMeta(head, "novel:update_time").toLocalDateTime()
        book.lastChapter = getOgMeta(head, "novel:latest_chapter_name")

        val stub = soup.selectFirst("div.articleInfo")
        book.words = stub.selectFirst("ol strong:eq(6)").text()
        book.intro = textOf(stub.selectFirst("dd").ownText())
        getContents(book, stub.selectFirst("a.reader").absUrl("href"), settings)
        return book
    }

    private fun getContents(book: Book, url: String, settings: Settings?) {
        val soup = fetchSoup(url, "get", settings)
        for (a in soup.select("div.readerListShow a")) {
            val chapter = book.newChapter(a.text())
            chapter.setText(a.absUrl("href"), settings)
        }
    }

    override fun getText(url: String, settings: Settings?): String =
            fetchSoup(url, "get", settings).selectText("div#content", System.lineSeparator())
}
