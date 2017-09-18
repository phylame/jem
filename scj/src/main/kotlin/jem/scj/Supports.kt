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

import jclp.io.createRecursively
import jclp.log.Log
import jclp.log.LogLevel
import jclp.setting.delegate
import jclp.text.Converter
import jclp.text.Converters
import jem.COVER
import jem.TITLE
import mala.App
import mala.AppSettings
import mala.AppVerbose
import java.io.File
import java.util.*

object SCISettings : AppSettings("app.cfg") {
    init {
        Converters[LogLevel::class.java] = object : Converter<LogLevel> {
            override fun render(obj: LogLevel) = obj.name

            override fun parse(str: String) = LogLevel.valueOf(str.toUpperCase())
        }
        Converters[AppVerbose::class.java] = object : Converter<AppVerbose> {
            override fun render(obj: AppVerbose) = obj.name

            override fun parse(str: String) = AppVerbose.valueOf(str.toUpperCase())
        }
    }

    var logLevel by delegate(Log.level, "app.logLevel")

    var appLocale by delegate(Locale.getDefault(), "app.locale")

    var appVerbose by delegate(App.verbose, "app.verbose")

    var enablePlugin by delegate(true, "app.plugin.enable")

    var termWidth by delegate(80, "app.termWidth")

    var pluginBlacklist: Collection<String>
        get() = File(App.home, "blacklist").takeIf(File::exists)?.readLines() ?: emptyList()
        set(paths) {
            File(App.home, "blacklist").let {
                if (it.exists() || it.parentFile.createRecursively()) {
                    it.writeText(paths.joinToString("\n"))
                } else {
                    App.error("cannot create")
                }
            }
        }

    var enableEpm by delegate(true, "jem.enableEpm")

    var enableCrawler by delegate(true, "jem.enableCrawler")

    var outputFormat by delegate("pmab", "jem.out.format")

    var separator by delegate("\n", "sci.view.separator")

    var skipEmpty by delegate(true, "sci.view.skipEmpty")

    var tocIndent by delegate("  ", "sci.view.tocIndent")

    var tocNames
        inline get() = (get("sci.view.tocNames") as? String)?.split(",") ?: listOf(TITLE, COVER)
        inline set(value) {
            set("sci.view.tocNames", value.joinToString(","))
        }

    fun viewSettings() = ViewSettings(separator, skipEmpty, tocIndent, tocNames)
}
