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

package jem.scj.addon

import jclp.VariantMap
import jem.scj.SCI
import jem.scj.SCISettings
import mala.App
import mala.App.tr
import mala.Describable
import mala.cli.*
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Option

class AppInspector : SCJAddon() {
    override val name = "App Inspector"

    override val description = tr("addon.inspector.desc")

    override fun init() {
        attachTranslator()

        val itemIndent = SCISettings["addon.inspector.itemIndent"] ?: " "

        Option.builder()
                .longOpt("view-context")
                .desc(tr("opt.viewContext.desc"))
                .command {
                    SCI.context.takeIf(AppContext::isNotEmpty)?.let {
                        for ((k, v) in it) {
                            println("$itemIndent$k[${v.javaClass.name}]=$v")
                        }
                    } ?: App.echo(tr("viewContext.empty"))
                    0
                }

        Option.builder()
                .longOpt("view-settings")
                .desc(tr("opt.viewSettings.desc"))
                .command {
                    println(tr("viewSettings.legend", SCISettings.file))
                    for ((k, v) in SCISettings) {
                        println("$itemIndent$k=$v")
                    }
                    0
                }

        Option.builder()
                .longOpt("list-plugins")
                .desc(tr("opt.listPlugins.desc"))
                .command {
                    val plugins = App.plugins.iterator().asSequence().map {
                        if (it is Describable)
                            "$itemIndent${it.name}\tv${it.version}\t${it.javaClass.name}"
                        else
                            "$itemIndent${it.javaClass.name}"
                    }.toList()
                    println(tr("listPlugins.legend", plugins.size))
                    println(plugins.joinToString("\n"))
                    0
                }

        newOption("S")
                .numberOfArgs(2)
                .valueSeparator()
                .argName(tr("opt.arg.kv"))
                .action(object : SingleFetcher() {
                    @Suppress("UNCHECKED_CAST")
                    override fun init(context: AppContext, cmd: CommandLine) {
                        SCISettings.update(cmd.getOptionProperties("S") as VariantMap)
                    }
                })
    }
}
