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

package jem.scj.app

import jclp.io.PathUtils
import jclp.log.Level
import jclp.log.Log
import jem.Book
import jem.crawler.CrawlerManager
import jem.epm.EpmManager
import jem.util.Build
import mala.cli.*
import mala.core.App
import mala.core.App.tr
import mala.core.AppVerbose
import mala.core.Plugin
import org.apache.commons.cli.*
import java.util.*
import kotlin.collections.HashMap

fun main(args: Array<String>) {
    App.run(SCI, args)
}

object SCI : CLIDelegate(DefaultParser()) {
    private const val TAG = "SCI"

    override val version = Build.VERSION

    override val name = "scj"

    @Suppress("UNCHECKED_CAST")
    val inArguments by lazy { context["p"] as? MutableMap<String, Any> ?: HashMap() }

    @Suppress("UNCHECKED_CAST")
    val outArguments by lazy { context["m"] as? MutableMap<String, Any> ?: HashMap() }

    @Suppress("UNCHECKED_CAST")
    val outAttributes by lazy { context["a"] as? MutableMap<String, Any> ?: HashMap() }

    @Suppress("UNCHECKED_CAST")
    val outExtensions by lazy { context["e"] as? MutableMap<String, Any> ?: HashMap() }

    val outputFormat get() = context["t"]?.toString() ?: SCISettings.outputFormat

    val output get() = context["o"]?.toString() ?: "."

    override fun onStart() {
        initApp()
        initJem()
        initOptions()
    }

    override fun onOptionError(e: ParseException) {
        val msg = when (e) {
            is UnrecognizedOptionException -> tr("error.opt.unknown", e.option)
            is AlreadySelectedException -> tr("error.opt.selected", e.option.opt, e.optionGroup.selected)
            is MissingArgumentException -> tr("error.opt.noArg", e.option.opt)
            is MissingOptionException -> tr("error.opt.noOption", e.missingOptions.joinToString(", "))
            else -> return
        }
        App.die(msg)
    }

    fun processInputs(processor: InputProcessor): Int {
        if (inputs.isEmpty()) {
            App.error(tr("error.input.empty"))
            return -1
        }
        var code = 0
        Log.d(TAG, "app context: {0}", context)
        Log.d(TAG, "app inputs: {0}", inputs)
        for (input in inputs) {
            val format = context["f"]?.toString() ?: EpmManager.formatOfFile(input) ?: PathUtils.extName(input)
            code = if (checkInputFormat(format, input)) {
                minOf(code, if (processor.process(input, format)) 0 else -1)
            } else {
                -1
            }
        }
        return code
    }

    private fun initApp() {
        Log.setLevel(SCISettings.logLevel)
        App.verbose = SCISettings.appVerbose
        Locale.setDefault(SCISettings.appLocale)
        App.translator = App.resourceManager.linguistFor("i18n/app")

        App.pluginManager.isEnable = SCISettings.enablePlugin
        if (App.pluginManager.isEnable) {
            App.pluginManager.loader = javaClass.classLoader
            App.pluginManager.blacklist = SCISettings.pluginBlacklist
        }
    }

    private fun initJem() {
        if (SCISettings.enableEpm) {
            EpmManager.loadImplementors()
        }
        if (SCISettings.enableCrawler) {
            CrawlerManager.loadCrawlers()
        }
    }

    private fun initOptions() {
        appOptions()
        jemOptions()
    }

    private fun appOptions() {
        newOption("h", "help").action { _ ->
            val helpFormatter = HelpFormatter()
            helpFormatter.syntaxPrefix = ""
            helpFormatter.descPadding = 4
            helpFormatter.printHelp(SCISettings.termWidth,
                    tr("opt.syntax", name),
                    tr("opt.header"),
                    options,
                    tr("opt.footer", Build.AUTHOR_EMAIL))
            App.exit(0)
        }

        newOption("v", "version").action { _ ->
            println("SCI for Jem v${Build.VERSION} on ${System.getProperty("os.name")}")
            println("(C) ${Calendar.getInstance()[Calendar.YEAR]} ${Build.VENDOR}")
            App.exit(0)
        }

        Option.builder("L")
                .hasArg()
                .longOpt("log-level")
                .argName(tr("opt.arg.level"))
                .desc(tr("opt.L.desc", Level.values().joinToString(", "), Log.getLevel()))
                .action { _, cmd ->
                    val value = cmd.getOptionValue("L")
                    try {
                        Log.setLevel(Level.valueOf(value))
                    } catch (e: IllegalArgumentException) {
                        App.die(tr("error.misc.badLogLevel", value))
                    }
                }

        Option.builder("V")
                .hasArg()
                .longOpt("verbose-level")
                .argName(tr("opt.arg.level"))
                .desc(tr("opt.V.desc", AppVerbose.values().joinToString(", "), SCISettings.appVerbose))
                .action { _, cmd ->
                    val value = cmd.getOptionValue("V")
                    try {
                        App.verbose = (AppVerbose.valueOf(value))
                    } catch (e: IllegalArgumentException) {
                        App.die(tr("error.misc.badVerbose", value))
                    }
                }
    }

    private fun jemOptions() {
        newOption("l", "list-formats").action { _ ->
            println(tr("list.legend"))
            println(tr("list.input", EpmManager.supportedParsers().joinToString(" ")))
            println(tr("list.output", EpmManager.supportedMakers().joinToString(" ")))
            App.exit(0)
        }

        newOption("f", "input-format")
                .hasArg()
                .argName(tr("opt.arg.format"))
                .action(RawFetcher("f") {
                    if (!checkInputFormat(it)) {
                        App.exit(-1)
                    }
                    true
                })

        valuesOptions("p")
        valuesOptions("a")
        valuesOptions("e")
        valuesOptions("m")

        valueOption("o", "output")

        Option.builder("t")
                .hasArg()
                .longOpt("output-format")
                .argName(tr("opt.arg.format"))
                .desc(tr("opt.t.desc", SCISettings.outputFormat))
                .action(RawFetcher("t") {
                    if (!checkOutputFormat(it)) {
                        App.exit(-1)
                    }
                    true
                })

        val group = OptionGroup()

        newOption("j").action(JoinBook()).group(group)

        Option.builder("c")
                .desc(tr("opt.c.desc", "-t", "-o"))
                .action(ConvertBook())
                .group(group)

        newOption("x")
                .hasArg()
                .argName(tr("opt.x.arg"))
                .action(ExtractBook())
                .group(group)

        val action = ViewBook()
        Option.builder("w")
                .hasArg()
                .argName(tr("opt.w.arg"))
                .desc(tr("opt.w.desc", "-w names"))
                .action(action)
                .group(group)

        defaultCommand = action
        options.addOptionGroup(group)
    }

    fun valueOption(opt: String, longOpt: String? = null) {
        newOption(opt, longOpt).hasArg().argName(tr("opt.$opt.arg")).action(RawFetcher(opt))
    }

    fun valuesOptions(opt: String) {
        newOption(opt).numberOfArgs(2).argName(tr("opt.arg.kv")).action(PropertiesFetcher(opt))
    }

    fun newOption(opt: String, longOpt: String? = null): Option.Builder = Option.builder(opt)
            .longOpt(longOpt)
            .desc(tr("opt.$opt.desc"))
}

interface SCIPlugin : Plugin {
    fun onOpenBook(param: EpmInParam) {}

    fun onOpenFailed(param: EpmInParam, e: Exception) {}

    fun onBookOpened(book: Book) {}

    fun onSaveBook(param: EpmOutParam) {}

    fun onSaveFailed(param: EpmOutParam, e: Exception) {}
}

fun App.sciAction(action: SCIPlugin.() -> Unit): Unit {
    try {
        pluginManager.with(SCIPlugin::class.java, action)
    } catch (e: Exception) {
        error(tr("error.misc.badPlugin"), e)
    }
}
