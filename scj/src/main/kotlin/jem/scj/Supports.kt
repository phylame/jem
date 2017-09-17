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

import jclp.log.Level
import jclp.log.Log
import jclp.text.ConverterManager
import jclp.value.Lazy
import jem.Attributes.COVER
import jem.Attributes.TITLE
import mala.core.App
import mala.core.AppSettings
import mala.core.AppVerbose
import mala.core.map
import java.io.File
import java.util.*

object SCISettings : AppSettings("app.cfg") {
    init {
        ConverterManager.registerParser(Level::class.java)
        ConverterManager.registerParser(AppVerbose::class.java)
    }

    var logLevel: Level by map(Lazy { Log.getLevel() }, "app.logLevel")

    var appLocale: Locale by map(Lazy { Locale.getDefault() }, "app.locale")

    var appVerbose: AppVerbose by map(Lazy { App.verbose }, "app.verbose")

    var enablePlugin by map(true, "app.plugin.enable")

    var termWidth by map(80, "app.termWidth")

    var pluginBlacklist: Collection<String>
        get() {
            val file = File(App.pathOf("blacklist"))
            return if (file.exists()) file.readLines() else emptySet()
        }
        set(paths) {
            if (App.initAppHome()) {
                File(App.pathOf("blacklist")).writeText(paths.joinToString("\n"))
            } else {
                App.error("cannot create settings home: ${App.pathOf("settings")}")
            }
        }

    var enableEpm by map(true, "jem.enableEpm")

    var enableCrawler by map(true, "jem.enableCrawler")

    var outputFormat by map("pmab", "jem.out.format")

    var separator by map("\n", "sci.view.separator")

    var skipEmpty by map(true, "sci.view.skipEmpty")

    var tocIndent by map("\t", "sci.view.tocIndent")

    var tocNames
        get() = (get("sci.view.tocNames") as? String)?.split(",") ?: listOf(TITLE, COVER)
        set(value) {
            set("sci.view.tocNames", value.joinToString(","))
        }

    fun asConfig() = ViewConfig(separator, skipEmpty, tocIndent, tocNames)
}
