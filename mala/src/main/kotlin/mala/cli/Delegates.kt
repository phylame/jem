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

package mala.cli

import mala.App
import mala.AppDelegate
import org.apache.commons.cli.*

abstract class CDelegate(private val parser: CommandLineParser) : AppDelegate {
    val options = Options()

    var inputs = emptyList<String>()
        private set

    val context = hashMapOf<String, Any>()

    private val commands = LinkedHashSet<Command>()

    private val actions = hashMapOf<String, Action>()

    protected var defaultCommand: Command? = null

    protected open fun onError(e: ParseException) {
        App.die(when (e) {
            is UnrecognizedOptionException -> App.tr("err.opt.unknown", e.option)
            is AlreadySelectedException -> App.tr("err.opt.selected", e.option.opt, e.optionGroup.selected)
            is MissingArgumentException -> App.tr("err.opt.noArg", e.option.opt)
            is MissingOptionException -> App.tr("err.opt.noOption", e.missingOptions.joinToString(", "))
            else -> return
        })
    }

    protected open fun onReady() = true

    fun addAction(option: Option, action: Action) {
        options.addOption(option)
        actions[option.opt ?: option.longOpt] = action
    }

    fun addCommand(option: Option, command: (CDelegate) -> Int) {
        addAction(option, object : Command {
            override fun execute(delegate: CDelegate) = command(delegate)
        })
    }

    fun addInitializer(option: Option, initializer: (AppContext, CommandLine) -> Unit) {
        addAction(option, object : Initializer {
            override fun perform(context: AppContext, cmd: CommandLine) = initializer(context, cmd)
        })
    }

    operator fun get(name: String) = context[name]

    operator fun set(name: String, value: Any) = context.set(name, value)

    override final fun run() {
        parseOptions()
        App.exit(if (onReady()) executeCommands() else -1)
    }

    private fun parseOptions() {
        try {
            val cmd = parser.parse(options, App.arguments)
            cmd.options.map {
                actions[it.opt ?: it.longOpt]
            }.forEach {
                if (it is Initializer) {
                    it.perform(context, cmd)
                }
                if (it is Command) {
                    commands += it
                }
            }
            inputs = cmd.args.asList()
        } catch (e: ParseException) {
            onError(e)
        }
    }

    private fun executeCommands(): Int {
        var code = 0
        if (commands.isNotEmpty()) {
            for (command in commands) {
                code = minOf(code, command.execute(this))
            }
        } else {
            code = defaultCommand?.execute(this) ?: 0
        }
        return code
    }
}

private typealias OB = Option.Builder

fun Option.group(group: OptionGroup): Option = apply {
    group.addOption(this)
}

fun OB.action(action: Action): Option = build().apply {
    (App.delegate as CDelegate).addAction(this, action)
}

fun OB.command(command: (CDelegate) -> Int): Option = build().apply {
    (App.delegate as CDelegate).addCommand(this, command)
}

fun OB.initializer(initializer: (AppContext, CommandLine) -> Unit): Option = build().apply {
    (App.delegate as CDelegate).addInitializer(this, initializer)
}

fun newOption(opt: String, longOpt: String? = null): OB = Option.builder(opt)
        .longOpt(longOpt)
        .desc(App.tr("opt.$opt.desc"))

fun valueOption(opt: String, longOpt: String? = null) = newOption(opt, longOpt)
        .hasArg()
        .argName(App.tr("opt.$opt.arg"))
        .action(StringFetcher(opt))

fun valuesOptions(opt: String, longOpt: String? = null) = newOption(opt, longOpt)
        .numberOfArgs(2)
        .valueSeparator()
        .argName(App.tr("opt.arg.kv"))
        .action(PropertiesFetcher(opt))
