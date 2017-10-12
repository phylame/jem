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

package mala

import jclp.io.createRecursively
import jclp.log.Log
import jclp.log.LogLevel
import jclp.setting.MapSettings
import jclp.setting.delegate
import jclp.text.Converter
import jclp.text.Converters
import java.io.File
import java.util.*

interface Describable {
    val name: String

    val version: String

    val description: String

    val vendor: String
}

open class MalaSettings(name: String, load: Boolean = true, sync: Boolean = true) : MapSettings() {
    val file = File(App.home, name)

    init {
        if (load && file.exists()) {
            @Suppress("LeakingThis")
            file.reader().use(this::load)
        }
        if (sync) {
            App.registerCleanup({
                if (file.exists() || file.parentFile.createRecursively()) {
                    sync(file.writer())
                } else {
                    App.error("cannot create directory for settings file: $file")
                }
            })
        }
    }

    companion object {
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
    }
}

open class AppSettings(name: String = "config/general.ini") : MalaSettings(name) {
    private val blacklist = File(App.home, "config/blacklist.txt")

    init {
        App.registerCleanup({
            if (blacklist.exists() || blacklist.parentFile.createRecursively()) {
                blacklist.bufferedWriter().use { out ->
                    pluginBlacklist.forEach { out.append(it).append("\n") }
                }
            } else {
                App.error("cannot create file for plugin blacklist")
            }
        })
    }

    var logLevel by delegate(Log.level, "app.log.level")

    var appVerbose by delegate(App.verbose, "app.verbose")

    var appLocale by delegate(Locale.getDefault(), "app.locale")

    var enablePlugin by delegate(true, "app.plugin.enable")

    val pluginBlacklist by lazy {
        hashSetOf<String>().apply {
            blacklist.takeIf(File::exists)?.useLines { this += it }
        }
    }
}
