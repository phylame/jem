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

package mala

import jclp.io.exists
import jclp.log.Log
import jclp.log.LogLevel
import jclp.setting.MapSettings
import jclp.setting.settingsWith
import jclp.text.Converter
import jclp.text.ConverterManager
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

interface Describable {
    val name: String

    val version: String

    val description: String

    val vendor: String
}

open class MalaSettings(name: String, load: Boolean = true, sync: Boolean = true) : MapSettings() {
    val file = Paths.get(App.home, name)

    init {
        if (load && file.exists) {
            Files.newBufferedReader(file).use { load(it) }
        }
        if (sync) {
            App.registerCleanup {
                if (Files.notExists(file.parent)) {
                    try {
                        Files.createDirectories(file.parent)
                        Files.newBufferedWriter(file).use { sync(it) }
                    } catch (e: Exception) {
                        App.error("cannot create directory for settings file: $file", e)
                    }
                } else {
                    Files.newBufferedWriter(file).use { sync(it) }
                }
            }
        }
    }

    companion object {
        init {
            ConverterManager[LogLevel::class.java] = object : Converter<LogLevel> {
                override fun render(value: LogLevel) = value.name

                override fun parse(text: String) = LogLevel.valueOf(text.toUpperCase())
            }
            ConverterManager[AppVerbose::class.java] = object : Converter<AppVerbose> {
                override fun render(value: AppVerbose) = value.name

                override fun parse(text: String) = AppVerbose.valueOf(text.toUpperCase())
            }
        }
    }
}

open class AppSettings(name: String = "config/general.ini") : MalaSettings(name) {
    private val blacklist = Paths.get(App.home, "config/blacklist.txt")

    init {
        App.registerCleanup {
            if (Files.notExists(blacklist.parent)) {
                try {
                    Files.createDirectories(blacklist.parent)
                    Files.write(blacklist, pluginBlacklist)
                } catch (e: Exception) {
                    App.error("cannot create file for plugin blacklist: $blacklist", e)
                }
            } else {
                Files.write(blacklist, pluginBlacklist)
            }
        }
    }

    var logLevel by settingsWith(Log.level, "app.log.level")

    var appVerbose by settingsWith(App.verbose, "app.verbose")

    var appLocale by settingsWith(Locale.getDefault(), "app.locale")

    var enablePlugin by settingsWith(true, "app.plugin.enable")

    val pluginBlacklist by lazy {
        hashSetOf<String>().apply {
            try {
                this += Files.readAllLines(blacklist)
            } catch (e: Exception) {
                App.error("cannot load plugin blacklist: $blacklist", e)
            }
        }
    }
}
