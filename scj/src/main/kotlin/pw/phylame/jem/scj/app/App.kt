/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
 *
 * This file is part of SCJ.
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

package pw.phylame.jem.scj.app

import org.apache.commons.cli.*
import pw.phylame.jem.epm.Helper
import pw.phylame.jem.epm.Registry
import pw.phylame.qaf.cli.*
import pw.phylame.qaf.core.App
import pw.phylame.qaf.core.Settings
import pw.phylame.qaf.core.Translator
import pw.phylame.qaf.core.tr
import pw.phylame.ycl.io.PathUtils
import pw.phylame.ycl.util.DateUtils
import java.io.File
import java.util.*

const val NAME = "scj"
const val VERSION = "3.0.0"

const val I18N_NAME = "pw/phylame/jem/scj/res/i18n/scj"
const val DATE_FORMAT = "yyyy-M-d"

// view key
const val VIEW_CHAPTER = "^chapter([\\-\\d\\.]+)(\\$.*)?"
const val VIEW_ITEM = "^item\\$.*"
const val VIEW_ALL = "all"
const val VIEW_TOC = "toc"
const val VIEW_EXTENSION = "ext"
const val VIEW_NAMES = "names"
const val VIEW_TEXT = "text"
const val VIEW_SIZE = "size"

// debug level
const val DEBUG_NONE = "none"
const val DEBUG_ECHO = "echo"
const val DEBUG_TRACE = "trace"

// CLI inTuple
const val OPTION_HELP = "h"
const val OPTION_VERSION = "v"
const val OPTION_LIST = "l"
const val OPTION_DEBUG_LEVEL = "d"
const val OPTION_INPUT_FORMAT = "f"
const val OPTION_OUTPUT_FORMAT = "t"
const val OPTION_OUTPUT = "o"
const val OPTION_EXTRACT = "x"
const val OPTION_VIEW = "w"
const val OPTION_JOIN = "j"
const val OPTION_CONVERT = "c"
const val OPTION_ATTRIBUTES = "a"
const val OPTION_EXTENSIONS = "e"
const val OPTION_PARSE_ARGUMENTS = "p"
const val OPTION_MAKE_ARGUMENTS = "m"
const val OPTION_LIST_NOVELS = "N"
const val OPTION_LIST_NOVELS_LONG = "novels"


object AppConfig : Settings() {
    override fun reset() {
        super.reset()
        comment = "Configurations of PW's SCJ v$VERSION\nUpdated: ${DateUtils.toISO(Date())}"

        appLocale = appLocale
        pluginEnable = pluginEnable
        debugLevel = debugLevel
        outputFormat = outputFormat
        viewKeys = viewKeys
        tocIndent = tocIndent
    }

    var appLocale by delegated(Locale.getDefault(), "app.locale")

    var pluginEnable by delegated(true, "app.plugin.enable")

    var debugLevel by delegated(DEBUG_ECHO, "app.debug.level")

    var outputFormat by delegated(Helper.PMAB, "jem.output.defaultFormat")

    var viewKeys by delegated(arrayOf(VIEW_ALL), "sci.view.defaultKey")

    var tocIndent by delegated("  ", "sci.view.tocIndent")
}

fun checkInputFormat(format: String): Boolean =
        if (!Registry.hasParser(format)) {
            App.error(tr("error.input.unsupported", format))
            println(tr("tip.unsupportedFormat"))
            false
        } else true

fun checkOutputFormat(format: String): Boolean =
        if (!Registry.hasMaker(format)) {
            App.error(tr("error.output.unsupported", format))
            println(tr("tip.unsupportedFormat"))
            false
        } else true

fun checkDebugLevel(level: String): Boolean {
    when (level) {
        DEBUG_ECHO -> App.debug = App.Debug.ECHO
        DEBUG_TRACE -> App.debug = App.Debug.TRACE
        DEBUG_NONE -> App.debug = App.Debug.NONE
        else -> {
            App.error(tr("error.invalidDebugLevel", level))
            return false
        }
    }
    return true
}

object SCI : CLIDelegate() {
    override fun onStart() {
        System.setProperty(Registry.AUTO_LOAD_CUSTOMIZED_KEY, "true")
        App.ensureHomeExisted()
        Locale.setDefault(AppConfig.appLocale)
        App.translator = Translator(I18N_NAME)
        if (!checkDebugLevel(AppConfig.debugLevel)) {
            App.exit(-1)
        }
        super.onStart()
        if (AppConfig.pluginEnable) {
            App.loadPlugins()
        }
    }

    override fun createOptions() {
        // help
        addOption(Option(OPTION_HELP, tr("help.description"))) {
            val formatter = HelpFormatter()
            formatter.syntaxPrefix = ""
            formatter.printHelp(
                    System.getProperty("sci.term.width")?.toInt() ?: 100,
                    tr("sci.syntax", App.assembly.name),
                    tr("help.prefix"),
                    options,
                    tr("help.feedback")
            )
            0
        }
        // version
        addOption(Option(OPTION_VERSION, tr("help.version"))) {
            println("SCI for Jem v$VERSION on ${System.getProperty("os.name")} (${System.getProperty("os.arch")})")
            println(tr("app.copyrights", Calendar.getInstance()[Calendar.YEAR].toString()))
            0
        }
        // list
        addOption(Option(OPTION_LIST, tr("help.list"))) {
            println(tr("list.title"))
            println(" ${tr("list.input")} ${Registry.supportedParsers().joinToString(" ")}")
            println(" ${tr("list.output")} ${Registry.supportedMakers().joinToString(" ")}")
            0
        }
        // debug level
        addOption("debug",
                Option.builder(OPTION_DEBUG_LEVEL)
                        .argName(tr("help.debug.argName"))
                        .hasArg()
                        .desc(tr("help.debug", AppConfig.debugLevel))
                        .build(),
                fetcherOf(OPTION_DEBUG_LEVEL, String::class.java, ::checkDebugLevel)
        )
        // input format
        addOption("inFormat",
                Option.builder(OPTION_INPUT_FORMAT)
                        .argName(tr("help.formatName"))
                        .hasArg()
                        .desc(tr("help.inputFormat"))
                        .build(),
                fetcherOf(OPTION_INPUT_FORMAT, String::class.java, ::checkInputFormat)
        )
        // parser arguments
        addOption("inArguments",
                Option.builder(OPTION_PARSE_ARGUMENTS)
                        .argName(tr("help.kvName"))
                        .numberOfArgs(2)
                        .valueSeparator()
                        .desc(tr("help.parserArgs"))
                        .build(),
                PropertiesFetcher(OPTION_PARSE_ARGUMENTS)
        )
        // output attributes
        addOption("outAttributes",
                Option.builder(OPTION_ATTRIBUTES)
                        .argName(tr("help.kvName"))
                        .numberOfArgs(2)
                        .valueSeparator()
                        .desc(tr("help.attribute"))
                        .build(),
                PropertiesFetcher(OPTION_ATTRIBUTES)
        )
        // output extensions
        addOption("outExtensions",
                Option.builder(OPTION_EXTENSIONS)
                        .argName(tr("help.kvName"))
                        .numberOfArgs(2)
                        .valueSeparator()
                        .desc(tr("help.extension"))
                        .build(),
                PropertiesFetcher(OPTION_EXTENSIONS)
        )
        // output path
        addOption("output",
                Option.builder(OPTION_OUTPUT)
                        .argName(tr("help.output.argName"))
                        .hasArg()
                        .desc(tr("help.output.path"))
                        .build(),
                fetcherOf<String>(OPTION_OUTPUT)
        )
        // output format
        addOption("outFormat",
                Option.builder(OPTION_OUTPUT_FORMAT)
                        .argName(tr("help.formatName"))
                        .hasArg()
                        .desc(tr("help.outputFormat", AppConfig.outputFormat))
                        .build(),
                fetcherOf(OPTION_OUTPUT_FORMAT, String::class.java, ::checkOutputFormat)
        )
        // maker arguments
        addOption("outArguments",
                Option.builder(OPTION_MAKE_ARGUMENTS)
                        .argName(tr("help.kvName"))
                        .numberOfArgs(2)
                        .valueSeparator()
                        .desc(tr("help.makerArgs"))
                        .build(),
                PropertiesFetcher(OPTION_MAKE_ARGUMENTS)
        )

        val group = OptionGroup()

        // convert
        var option = Option(OPTION_CONVERT, false, tr("help.convert", OPTION_OUTPUT_FORMAT))
        group.addOption(option)
        addOption(option, object : ConsumerCommand {
            override fun consume(tuple: InTuple): Boolean = convertBook(tuple, OutTuple())
        })


        // join
        option = Option(OPTION_JOIN, false, tr("help.join"))
        group.addOption(option)
        addOption(option) {
            if (inputs.isEmpty()) {
                App.error(tr("error.input.empty"))
                -1
            } else {
                if (joinBook(OutTuple(File(output)))) 0 else 1
            }
        }

        // extract and indices
        option = Option.builder(OPTION_EXTRACT).argName(tr("help.extract.argName")).hasArg().desc(tr("help.extract")).build()
        group.addOption(option)
        addOption("chapterIndices", option, ExtractBook(OPTION_EXTRACT))

        // view and names
        val viewBook = ViewBook(OPTION_VIEW)
        defaultCommand = viewBook
        addOption("viewKeys",
                Option.builder(OPTION_VIEW)
                        .argName(tr("help.view.argName"))
                        .hasArg()
                        .valueSeparator()
                        .desc(tr("help.view"))
                        .build(),
                viewBook
        )

        addOptionGroup(group)

        // list uc novels
        addOption(Option.builder(OPTION_LIST_NOVELS)
                .longOpt(OPTION_LIST_NOVELS_LONG)
                .desc(tr("help.ucnovels"))
                .build(),
                ListUCNovels()
        )
    }

    override fun onOptionError(e: ParseException) {
        when (e) {
            is UnrecognizedOptionException -> {
                App.error(tr("error.option.unrecognized", e.option))
            }
            is MissingArgumentException -> {
                App.error(tr("error.option.missingArgument", e.option.opt))
            }
            is AlreadySelectedException -> {
                App.error(tr("error.option.multiOptions", e.option.opt, e.optionGroup.selected))
            }
            is MissingOptionException -> {
                App.error(tr("error.option.missingOption", e.missingOptions.joinToString(",")))
            }
            else -> e.printStackTrace()
        }
        App.exit(-1)
    }

    val inFormat: String? by managed { null }

    val inArguments by managed { emptyMap<String, String>() }

    val outAttributes by managed { emptyMap<String, Any>() }

    val outExtensions by managed { emptyMap<String, Any>() }

    val output: String by managed { "." }

    val outFormat by managed { AppConfig.outputFormat }

    val outArguments by managed { emptyMap<String, Any>() }

    val chapterIndices by managed { emptyArray<String>() }

    val viewKeys by managed { AppConfig.viewKeys }

    fun consumeInputs(consumer: Consumer): Int {
        if (inputs.isEmpty()) {
            App.error(tr("error.input.empty"))
            return -1
        }
        var status = 0
        for (input in inputs) {
            val file = File(input)
            if (!file.exists()) {
                App.error(tr("error.input.notExists", input))
                status = -1
                continue
            }
            val format = inFormat ?: Helper.formatOfExtension(input) ?: PathUtils.extensionName(input)
            if (!checkInputFormat(format)) {
                status = -1
                continue
            }
            status = Math.min(status, if (consumer.consume(InTuple(file, format))) 0 else 1)
        }
        return status
    }
}

data class InTuple(
        val file: File,
        val format: String,
        val arguments: Map<String, Any> = SCI.inArguments
)

data class OutTuple(
        val output: File = File(SCI.output),
        val format: String = SCI.outFormat,
        val arguments: Map<String, Any> = SCI.outArguments,
        val attributes: Map<String, Any> = SCI.outAttributes,
        val extensions: Map<String, Any> = SCI.outExtensions
)

interface Consumer {
    fun consume(tuple: InTuple): Boolean
}

interface ConsumerCommand : Command, Consumer {
    override fun execute(delegate: CLIDelegate): Int = SCI.consumeInputs(this)
}

class ExtractBook(option: String) : ListFetcher(option), ConsumerCommand {
    override fun consume(tuple: InTuple): Boolean {
        var state = true
        @Suppress("unchecked_cast")
        for (index in SCI.chapterIndices) {
            state = extractBook(tuple, index, OutTuple())
        }
        return state
    }
}

class ViewBook(option: String) : ListFetcher(option), ConsumerCommand {
    override fun consume(tuple: InTuple): Boolean = @Suppress("unchecked_cast") viewBook(tuple, OutTuple(), SCI.viewKeys)
}

class ListUCNovels : Command {
    override fun execute(delegate: CLIDelegate): Int {
        TODO("under development")
    }
}

fun main(args: Array<String>) {
    App.run(NAME, VERSION, args, SCI)
}
