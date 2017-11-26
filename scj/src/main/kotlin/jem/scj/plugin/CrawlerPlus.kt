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

package jem.scj.plugin

import jclp.log.Log
import jclp.setting.getInt
import jem.Chapter
import jem.crawler.CRAWLER_LISTENER_KEY
import jem.crawler.CrawlerManager
import jem.crawler.CrawlerText
import jem.crawler.TextListener
import jem.epm.MakerParam
import jem.epm.ParserParam
import jem.scj.SCI
import jem.scj.SCISettings
import jem.scj.SCJPlugin
import jem.title
import mala.App
import mala.App.tr
import mala.cli.ValueSwitcher
import mala.cli.action
import mala.cli.command
import org.apache.commons.cli.Option
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class CrawlerPlus : SCJAddon(), SCJPlugin, TextListener {
    override val name = "CrawlerPlus"

    override val description = "Utilities for Jem Crawler"

    override fun init() {
        Option.builder()
                .longOpt("list-crawlers")
                .desc(tr("opt.listCrawlers.desc"))
                .command {
                    println(tr("listCrawlers.legend"))
                    CrawlerManager.services.forEach {
                        println(tr("listCrawlers.name", it.name))
                        println(tr("listCrawlers.hosts", it.keys.joinToString(", ")))
                        println("-".repeat(64))
                    }
                    0
                }

        Option.builder()
                .longOpt("parallel")
                .desc(tr("opt.parallel.desc"))
                .action(ValueSwitcher("parallel"))
    }

    private val threadPool = lazy {
        val threads = SCISettings.getInt("crawler.maxThread") ?: Runtime.getRuntime().availableProcessors() * 8
        Executors.newFixedThreadPool(threads)
    }

    private val printCenter = lazy { Executors.newSingleThreadExecutor() }

    override fun onOpenBook(param: ParserParam) {
        if (SCI.context["parallel"] == true && param.epmName == "crawler") {
            param.arguments?.set(CRAWLER_LISTENER_KEY, this)
        }
    }

    override fun onSaveBook(param: MakerParam) {
        if (SCI.context["parallel"] == true) {
            Log.t("CrawlerPlus") { "parallel mode is enabled" }
            schedule(param.book, Local())
        }
    }

    private fun schedule(chapter: Chapter, data: Local) {
        (chapter.text as? CrawlerText)?.let {
            it.schedule(threadPool.value)
            data.totals.incrementAndGet()
            chapter.tag = data
        }
        for (stub in chapter) {
            schedule(stub, data)
        }
    }

    override fun onSuccess(url: String, chapter: Chapter) {
        printCenter.value.submit {
            val data = chapter.tag as Local
            App.echo("${data.current.getAndIncrement()}/${data.totals.get()}: ${chapter.title}")
            chapter.tag = null
        }
    }

    override fun destroy() {
        if (threadPool.isInitialized()) {
            threadPool.value.shutdown()
        }
        if (printCenter.isInitialized()) {
            printCenter.value.shutdown()
        }
    }

    private class Local {
        val totals = AtomicInteger()

        var current = AtomicInteger(1)
    }
}
