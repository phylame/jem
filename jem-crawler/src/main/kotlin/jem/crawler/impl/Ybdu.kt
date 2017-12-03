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
import jclp.io.htmlTrim
import jclp.setting.Settings
import jclp.text.textOf
import jclp.text.valueFor
import jclp.toLocalDateTime
import jem.*
import jem.crawler.*
import jem.crawler.M as T

class Ybdu : AbstractCrawler() {
    override val name = T.tr("ybdu.com")

    override val keys = setOf("www.ybdu.com")

    override fun getBook(url: String, settings: Settings?): Book {
        val path = url.replaceFirst("xiaoshuo", "xiazai")

        val book = CrawlerBook()
        book.sourceUrl = path
        book.sourceSite = "ybdu"
        book.bookId = baseName(path.removeSuffix("/"))
        val soup = fetchSoup(path, "get", settings)

        val stub = soup.selectFirst("div.ui_bg6")
        book.cover = CrawlerFlob(stub.selectImage("img")!!, "get", settings)
        book.state = stub.select("div.tLj:eq(8)").text().removeSurrounding("文章状态：", "中")
        book.title = stub.selectFirst("h1").text().removeSuffix("TXT下载")
        book.author = stub.selectFirst("p").ownText().valueFor("作者", " ", "：")!!
        book.genre = stub.selectFirst("table p:eq(2) a").text().removeSuffix("小说")
        book.intro = textOf(stub.selectFirst("div.intro").text().htmlTrim())
        book.updateTime = stub.selectFirst("p.ti em").text().split("：")[1].toLocalDateTime()
        book.lastChapter = stub.selectFirst("p.ti a").text()

        getContents(book, path.replaceFirst("xiazai", "xiaoshuo"), settings)
        return book
    }

    private fun getContents(book: Book, url: String, settings: Settings?) {
        val soup = fetchSoup(url, "get", settings)
        for (a in soup.select("ul.mulu_list a")) {
            val chapter = book.newChapter(a.text().replaceFirst("\\d+\\.".toRegex(), ""))
            val u = a.absUrl("href")
            chapter.text = CrawlerText(u, chapter, this, settings)
            chapter[ATTR_CHAPTER_SOURCE_URL] = u
        }
    }

    override fun getText(url: String, settings: Settings?): String {
        return fetchSoup(url, "get", settings).selectText("#htmlContent", System.lineSeparator())
    }
}
