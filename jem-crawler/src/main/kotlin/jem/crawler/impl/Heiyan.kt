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
import jclp.text.count
import jclp.text.textOf
import jclp.toLocalDateTime
import jem.*
import jem.crawler.*
import java.time.LocalDate
import jem.crawler.M as T

class Heiyan : ReusableCrawler() {
    override val name = T.tr("heiyan.com")

    override val keys = setOf("www.heiyan.com")

    override fun getBook(url: String, settings: Settings?): Book {
        val path = url.removeSuffix("/chapter")

        val book = CrawlerBook(path, "heiyan")
        book.bookId = baseName(path)

        val soup = fetchSoup(path, "get", settings)

        val head = soup.head()
        book.title = getOgMeta(head, "title")
        book.author = getOgMeta(head, "novel:author")
        book.state = getOgMeta(head, "novel:status")
        book.genre = getOgMeta(head, "novel:category")
        book.cover = CrawlerFlob(getOgMeta(head, "image").let { it.substring(0, it.indexOf('@')) }, "get", settings)
        book.updateTime = getOgMeta(head, "novel:update_time").let {
            (if (it.count('-') == 2) "$it:00" else "${LocalDate.now().year}-$it:00").toLocalDateTime()
        }
        book.lastChapter = getOgMeta(head, "novel:latest_chapter_name")

        val stub = soup.selectFirst("div.pattern-cover-detail")
        book.words = stub.selectFirst("span.words").text().removeSuffix("字")
        book.intro = textOf(stub.selectFirst("pre").text().trim().replace("(\\s|\u3000)+".toRegex(), System.lineSeparator()))

        getContents(book, "$path/chapter", settings)
        return book
    }

    override fun getText(url: String, settings: Settings?): String {
        return fetchSoup(url, "get", settings).selectText("div.page-content p", System.lineSeparator())
    }

    private fun getContents(book: Book, url: String, settings: Settings?) {
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
                    chapter.setText(a.absUrl("href").replace("www", "w"), settings)
                }
            }
        }
    }
}
