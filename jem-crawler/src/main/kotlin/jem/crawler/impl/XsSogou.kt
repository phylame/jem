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
import jem.*
import jem.crawler.*
import jem.crawler.M as T

class XsSogou : ReusableCrawler() {
    override val name = T.tr("xs.sogou.com")

    override val keys = setOf("xs.sogou.com")

    override fun getBook(url: String, settings: Settings?): Book {
        val path = url.replaceFirst("list", "book")

        val book = CrawlerBook(path, "xssogou")
        val bookId = baseName(path).apply { book.bookId = this }

        val soup = fetchSoup(path, "get", settings)

        var stub = soup.selectFirst("div.info-box")
        book.cover = CrawlerFlob(stub.selectImage("img")!!, "get", settings)
        stub = stub.selectFirst("div.infos")
        book.title = stub.selectFirst("h1").text()
        book.author = stub.select("span.fl:eq(0)").text().substring(3)
        book.genre = stub.select("span.fl:eq(1)").text().substring(3)
        book.state = stub.select("span.fl:eq(2)").text().substring(3)
        book.words = stub.select("span.fl:eq(3)").text().substring(3)
        book.intro = textOf((stub.selectFirst("div.desc-long") ?: stub.selectFirst("div.desc-short")).text())

        getContents(book, path.replace("book", "list"), settings)
        return book
    }

    override fun getText(url: String, settings: Settings?): String {
        return fetchSoup(url, "get", settings).selectText("#contentWp p", System.lineSeparator())
    }

    private fun getContents(book: Book, url: String, settings: Settings?) {
        val soup = fetchSoup(url, "get", settings)
        var section: Chapter? = null
        for (div in soup.select("div.box-content > div")) {
            val className = div.className()
            if ("chapter-box" in className) {
                for (a in div.select("a")) {
                    val chapter = (section ?: book).newChapter(a.text())
                    chapter.setText(a.absUrl("href"), settings)
                }
            } else if ("volume" in className) {
                section = book.newChapter(div.selectFirst("span").text())
            }
        }
    }
}
