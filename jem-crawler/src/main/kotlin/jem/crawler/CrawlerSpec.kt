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

package jem.crawler

import jclp.KeyedService
import jclp.ServiceManager
import jclp.setting.Settings
import jem.Book
import jem.Chapter
import jem.epm.EpmFactory
import jem.epm.Parser
import jem.epm.ParserException
import java.io.InterruptedIOException
import java.net.URL

const val ATTR_CHAPTER_UPDATE_TIME = "updateTime"
const val ATTR_CHAPTER_SOURCE_URL = "sourceUrl"

const val EXT_CRAWLER_BOOK_ID = "jem.ext.crawler.bookId"
const val EXT_CRAWLER_SOURCE_URL = "jem.ext.crawler.sourceUrl"
const val EXT_CRAWLER_SOURCE_SITE = "jem.ext.crawler.sourceSite"
const val EXT_CRAWLER_LAST_CHAPTER = "jem.ext.crawler.lastChapter"
const val EXT_CRAWLER_UPDATE_TIME = "jem.ext.crawler.updateTime"

interface Crawler {
    fun getBook(url: String, settings: Settings?): Book

    fun getText(url: String, settings: Settings?): String {
        throw NotImplementedError()
    }
}

interface CrawlerFactory : KeyedService {
    fun getCrawler(): Crawler
}

object CrawlerManager : ServiceManager<CrawlerFactory>(CrawlerFactory::class.java) {
    fun getCrawler(host: String) = get(host)?.getCrawler()

    fun fetchBook(url: String, settings: Settings?) = getCrawler(URL(url).host)?.getBook(url, settings)
}

abstract class ReusableCrawler : Crawler, CrawlerFactory {
    override fun getCrawler(): Crawler = this

    protected fun Chapter.setText(url: String, settings: Settings?) {
        text = CrawlerText(url, this, this@ReusableCrawler, settings)
        this[ATTR_CHAPTER_SOURCE_URL] = url
    }

    protected open fun fetchPage(page: Int, arg: Any): Int {
        throw NotImplementedError()
    }

    protected fun fetchContents(arg: Any) {
        for (i in 2 until fetchPage(1, arg)) {
            if (Thread.interrupted()) throw InterruptedIOException()
            fetchPage(i, arg)
        }
    }
}

class CrawlerParser : Parser, EpmFactory {
    override val name = "Book Crawler"

    override val keys = setOf("crawler", "net")

    override val hasParser = true

    override val parser = this

    override fun parse(input: String, arguments: Settings?): Book {
        return CrawlerManager.fetchBook(input, arguments) ?: throw ParserException(M.tr("err.crawler.unsupported", input))
    }
}
