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

import jem.scj.app.SCI
import jem.scj.app.SCISettings
import mala.cli.action
import mala.core.App
import mala.core.App.tr
import mala.core.Plugin
import org.apache.commons.cli.Option

class AppInspector : Plugin {
    override val meta = mapOf("name" to "App Inspector")

    override fun init() {
        Supports.attachTranslator()
        Option.builder()
                .longOpt("view-context")
                .desc(tr("opt.viewContext.desc"))
                .action { _ ->
                    val context = SCI.context
                    if (context.isEmpty()) {
                        App.echo(tr("viewContext.empty"))
                    } else {
                        for ((k, v) in context) {
                            println("$k[${v.javaClass.name}]=$v")
                        }
                    }
                    0
                }
        Option.builder()
                .longOpt("view-settings")
                .desc(tr("opt.viewSettings.desc"))
                .action { _ ->
                    println(tr("viewSettings.legend", SCISettings.file))
                    for ((k, v) in SCISettings) {
                        println("  $k=$v")
                    }
                    0
                }
        Option.builder()
                .longOpt("list-plugins")
                .desc(tr("opt.listPlugins.desc"))
                .action { _ ->
                    val plugins = App.pluginManager.toList().map {
                        "  ${it.meta["name"]}\t\t${it.javaClass.name}"
                    }
                    println(tr("listPlugins.legend", plugins.size))
                    println(plugins.joinToString("\n"))
                    0
                }
    }
}
