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

import jclp.ServiceManager
import jclp.ServiceProvider
import jclp.setting.Settings
import jem.Book
import jem.epm.EpmFactory
import jem.epm.Parser
import jem.epm.ParserException
import java.io.InterruptedIOException
import java.net.URL

class CrawlerBook : Book()

interface Crawler {
    fun getBook(url: String, settings: Settings?): Book

    fun getText(url: String, settings: Settings?): String {
        TODO()
    }
}

interface CrawlerFactory : ServiceProvider {
    fun getCrawler(): Crawler
}

object CrawlerManager : ServiceManager<CrawlerFactory>(CrawlerFactory::class.java) {
    fun getCrawler(host: String) = get(host)?.getCrawler()

    fun fetchBook(url: String, settings: Settings?) = getCrawler(URL(url).host)?.getBook(url, settings)
}

abstract class AbstractCrawler : Crawler, CrawlerFactory {
    override fun getCrawler(): Crawler = this

    protected open fun fetchPage(page: Int, arg: Any): Int {
        throw UnsupportedOperationException("Not Implemented")
    }

    protected fun fetchToc(arg: Any) {
        for (i in 2 until fetchPage(1, arg)) {
            if (Thread.interrupted()) {
                throw InterruptedIOException()
            }
            fetchPage(i, arg)
        }
    }
}

class CrawlerParser : EpmFactory, Parser {
    override val name = "Book Crawler"

    override val keys = setOf("crawler")

    override val parser = this

    override fun parse(input: String, arguments: Settings?): Book {
        return CrawlerManager.fetchBook(input, arguments)?.apply {
            set("source", input)
        } ?: throw ParserException(M.tr("err.crawler.unsupported", input))
    }
}
