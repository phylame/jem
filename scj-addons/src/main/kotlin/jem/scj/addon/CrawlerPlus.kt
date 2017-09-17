/*
 * Copyright 2017 Peng Wan <phylame@163.com>
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

package jem.scj.addon

import jem.Chapter
import jem.scj.SCI
import jem.scj.SCIPlugin
import jem.scj.SCISettings
import mala.App
import mala.cli.action
import org.apache.commons.cli.Option
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
//
//class CrawlerPlus : SCIPlugin, CrawlerListener {
//    private val threadPool = lazy {
//        val threads = SCISettings.get("crawler.maxThread", Int::class.java) as? Int
//        Executors.newFixedThreadPool(threads ?: Runtime.getRuntime().availableProcessors() * 8)
//    }
//
//    private val printCenter = lazy {
//        Executors.newSingleThreadExecutor()
//    }
//
//    private var totals: AtomicInteger = AtomicInteger()
//
//    private var current: AtomicInteger = AtomicInteger()
//
//    override val meta = mapOf("name" to "Crawler Plus")
//
//    init {
//        Supports.attachTranslator()
//        Option.builder()
//                .longOpt("list-crawlers")
//                .desc(tr("opt.listCrawlers.desc"))
//                .action { _ ->
//                    println(tr("listCrawlers.legend"))
//                    SCI.crawlerManager.services.forEach {
//                        println(tr("listCrawlers.name", it.name))
//                        println(tr("listCrawlers.hosts", it.names.joinToString(", ")))
//                        println("-" * 64)
//                    }
//                    App.exit(0)
//                }
//    }
//
//    override fun onOpenBook(param: ParserParam) {
//        if (param.format == "crawler") {
//            param.arguments?.set("crawler.listener", this)
//            param.arguments?.set("crawler.crawlerManager", SCI.crawlerManager)
//        }
//    }
//
//    override fun onSaveBook(param: MakerParam) {
//        val book = param.book
//        if (book is CrawlerBook) {
//            current.set(1)
//            book.schedule(threadPool.value)
//            totals.set(book.totals)
//        }
//    }
//
//    override fun destroy() {
//        if (threadPool.isInitialized()) {
//            threadPool.value.shutdown()
//        }
//        if (printCenter.isInitialized()) {
//            printCenter.value.shutdown()
//        }
//    }
//
//    override fun textFetched(chapter: Chapter, url: String) {
//        printCenter.value.submit {
//            println("${current.getAndIncrement()}/${totals.get()}: ${chapter.title}")
//        }
//    }
//}
