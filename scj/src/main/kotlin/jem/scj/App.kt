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

package jem.scj

import jclp.io.extName
import jclp.log.Log
import jclp.log.LogLevel
import jclp.setting.MapSettings
import jem.Book
import jem.Build
import jem.epm.EpmManager
import jem.epm.MakerParam
import jem.epm.ParserParam
import mala.App
import mala.App.tr
import mala.AppVerbose
import mala.Plugin
import mala.cli.*
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.OptionGroup
import java.lang.System.getProperty
import java.time.LocalDate

fun main(args: Array<String>) {
    App.run(SCI, args)
}

object SCI : CDelegate(DefaultParser()) {
    private const val TAG = "SCI"

    override val name = "scj"

    override val version = Build.VERSION

    @Suppress("UNCHECKED_CAST")
    val inArguments
        get() = MapSettings(context["p"] as? MutableMap<String, Any>)

    @Suppress("UNCHECKED_CAST")
    val outArguments
        get() = MapSettings(context["m"] as? MutableMap<String, Any>)

    @Suppress("UNCHECKED_CAST")
    val outAttributes
        get() = context["a"] as? MutableMap<String, Any> ?: hashMapOf()

    @Suppress("UNCHECKED_CAST")
    val outExtensions
        get() = context["e"] as? MutableMap<String, Any> ?: hashMapOf()

    val outputFormat get() = context["t"]?.toString() ?: SCISettings.outputFormat

    val output get() = context["o"]?.toString() ?: "."

    override fun onStart() {
        restoreState(SCISettings)
        appOptions()
        jemOptions()
    }

    override fun onStop() {
    }

    fun processInputs(processor: InputProcessor): Int {
        Log.d(TAG) { "app context: $context" }
        Log.d(TAG) { "app inputs: $inputs" }
        if (inputs.isEmpty()) {
            App.error(tr("err.input.empty"))
            return -1
        }
        var code = 0
        for (input in inputs) {
            val format = context["f"]?.toString() ?: extName(input)
            code = if (checkInputFormat(format, input)) {
                minOf(code, if (processor.process(input, format)) 0 else -1)
            } else {
                -1
            }
        }
        return code
    }

    private fun appOptions() {
        newOption("h", "help").command {
            with(HelpFormatter()) {
                descPadding = 4
                syntaxPrefix = tr("opt.syntaxPrefix")
                printHelp(SCISettings.termWidth,
                        tr("opt.syntax", name),
                        tr("opt.header", Build.VERSION),
                        options,
                        tr("opt.footer", Build.AUTHOR_EMAIL))
                App.exit(0)
            }
        }

        newOption("v", "version").command {
            println(tr("opt.v.appInfo", Build.VERSION, getProperty("os.name")))
            println(tr("opt.v.javaInfo", getProperty("java.version"), getProperty("os.arch"), getProperty("java.vendor")))
            println("(C) 2014-${LocalDate.now().year} ${Build.VENDOR}")
            App.exit(0)
        }

        Option.builder("L")
                .hasArg()
                .longOpt("log-level")
                .argName(tr("opt.arg.level"))
                .desc(tr("opt.L.desc", LogLevel.values().joinToString(", "), Log.level))
                .initializer { _, cmd ->
                    val value = cmd.getOptionValue("L")
                    try {
                        Log.level = LogLevel.valueOf(value.toUpperCase())
                    } catch (e: IllegalArgumentException) {
                        App.die(tr("err.misc.badLogLevel", value))
                    }
                }

        Option.builder("V")
                .hasArg()
                .longOpt("verbose-level")
                .argName(tr("opt.arg.level"))
                .desc(tr("opt.V.desc", AppVerbose.values().joinToString(", "), App.verbose))
                .initializer { _, cmd ->
                    val value = cmd.getOptionValue("V")
                    try {
                        App.verbose = AppVerbose.valueOf(value.toUpperCase())
                    } catch (e: IllegalArgumentException) {
                        App.die(tr("err.misc.badVerbose", value))
                    }
                }
    }

    private fun jemOptions() {
        newOption("l", "list-formats").command {
            println(tr("list.legend"))
            EpmManager.services.forEach {
                println(tr("list.name", it.name, tr("values.${it.hasMaker}"), tr("values.${it.hasParser}")))
                println(tr("list.keys", it.keys.joinToString(", ")))
                println("-".repeat(64))
            }
            App.exit(0)
        }

        newOption("f", "input-format")
                .hasArg()
                .argName(tr("opt.arg.format"))
                .action(StringFetcher("f") {
                    if (!checkInputFormat(it)) {
                        App.exit(-1)
                    }
                    true
                })

        Option.builder("t")
                .hasArg()
                .longOpt("output-format")
                .argName(tr("opt.arg.format"))
                .desc(tr("opt.t.desc", SCISettings.outputFormat))
                .action(StringFetcher("t") {
                    if (!checkOutputFormat(it)) {
                        App.exit(-1)
                    }
                    true
                })

        valuesOptions("p")
        valuesOptions("a")
        valuesOptions("e")
        valuesOptions("m")

        valueOption("o", "output")

        newOption("F", "force")
                .action(ValueSwitcher("F"))

        val group = OptionGroup()

        newOption("j")
                .action(JoinBook())
                .group(group)

        Option.builder("c")
                .desc(tr("opt.c.desc", "-t", "-o"))
                .action(ConvertBook())
                .group(group)

        newOption("x")
                .hasArg()
                .argName(tr("opt.x.arg"))
                .action(ExtractBook())
                .group(group)

        ViewBook().let {
            defaultCommand = it
            Option.builder("w")
                    .hasArg()
                    .argName(tr("opt.w.arg"))
                    .desc(tr("opt.w.desc", "-w names"))
                    .action(it)
                    .group(group)
        }

        options.addOptionGroup(group)
    }
}

interface SCJPlugin : Plugin {
    fun onOpenBook(param: ParserParam) {}

    fun onOpenFailed(e: Exception, param: ParserParam) {}

    fun onBookOpened(book: Book, param: ParserParam?) {}

    fun onSaveBook(param: MakerParam) {}

    fun onSaveFailed(e: Exception, param: MakerParam) {}

    fun onBookSaved(param: MakerParam) {}
}

inline fun App.scjAction(action: SCJPlugin.() -> Unit) {
    try {
        plugins.with(action)
    } catch (e: Exception) {
        error(tr("err.misc.badPlugin"), e)
    }
}
