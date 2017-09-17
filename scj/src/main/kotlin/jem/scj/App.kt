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

package jem.scj

import jclp.log.Log
import jem.Book
import jem.Build
import jem.epm.MakerParam
import jem.epm.ParserParam
import mala.App
import mala.Plugin
import mala.cli.CLIDelegate
import mala.cli.PropertiesFetcher
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.OptionGroup
import java.util.Calendar
import java.util.Locale
import kotlin.collections.HashMap

fun main(args: Array<String>) {
    App.run(SCI, args)
}

object SCI : CLIDelegate(DefaultParser()) {
    private const val TAG = "SCI"

    override val version = Build.VERSION

    override val name = "scj"

    @Suppress("UNCHECKED_CAST")
    val inArguments
        get() = context["p"] as? MutableMap<String, Any> ?: HashMap()

    @Suppress("UNCHECKED_CAST")
    val outArguments
        get() = context["m"] as? MutableMap<String, Any> ?: HashMap()

    @Suppress("UNCHECKED_CAST")
    val outAttributes
        get() = context["a"] as? MutableMap<String, Any> ?: HashMap()

    @Suppress("UNCHECKED_CAST")
    val outExtensions
        get() = context["e"] as? MutableMap<String, Any> ?: HashMap()

    val outputFormat get() = context["t"]?.toString() ?: SCISettings.outputFormat

    val output get() = context["o"]?.toString() ?: "."

//    val crawlerManager by lazy { CrawlerManager() }

    override fun onStart() {
        initApp()
        initJem()
        initOptions()
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
            val format = context["f"]?.toString() ?: PathUtils.extName(input)
            code = if (checkInputFormat(format, input)) {
                minOf(code, if (processor.process(input, format)) 0 else -1)
            } else {
                -1
            }
        }
        return code
    }

    fun nameOf(b: Boolean) = tr("values." + b)

    private fun initApp() {
        Log.setLevel(SCISettings.logLevel)
        App.verbose = SCISettings.appVerbose
        Locale.setDefault(SCISettings.appLocale)
        App.translator = App.resourceManager.linguistFor("i18n/app")

        val pm = App.pluginManager
        pm.isEnable = SCISettings.enablePlugin
        if (pm.isEnable) {
            pm.loader = javaClass.classLoader
            pm.blacklist = SCISettings.pluginBlacklist
        }
    }

    private fun initJem() {
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
            println("(C) 2014-${Calendar.getInstance()[Calendar.YEAR]} ${Build.VENDOR}")
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
            epmManager.services.forEach {
                println(tr("list.name", it.name, nameOf(it.hasMaker()), nameOf(it.hasParser())))
                println(tr("list.names", it.names.joinToString(", ")))
                println("-" * 64)
            }
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
    fun onOpenBook(param: ParserParam) {}

    fun onOpenFailed(e: Exception, param: ParserParam) {}

    fun onBookOpened(book: Book, param: ParserParam?) {}

    fun onSaveBook(param: MakerParam) {}

    fun onSaveFailed(e: Exception, param: MakerParam) {}

    fun onBookSaved(param: MakerParam) {}
}

fun App.sciAction(action: SCIPlugin.() -> Unit) {
    try {
        plugins.with(SCIPlugin::class.java, action)
    } catch (e: Exception) {
        error(tr("error.misc.badPlugin"), e)
    }
}
