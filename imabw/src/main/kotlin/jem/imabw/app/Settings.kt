/*
 * Copyright 2017 Peng Wan <phylame@163.com>
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

package jem.imabw.app

import pw.phylame.qaf.core.App
import pw.phylame.qaf.core.Settings
import pw.phylame.qaf.ixin.Ixin
import pw.phylame.ycl.io.IOUtils
import pw.phylame.ycl.log.LogLevel
import java.awt.Font
import java.util.*

object AppSettings : Settings("${SETTINGS_DIR}/settings") {
    override fun reset() {
        super.reset()
        comment = "Common settings for Imabw"

        appLocale = appLocale
        logLevel = logLevel
        historyEnable = historyEnable
        historyLimits = historyLimits
    }

    var appLocale: Locale by delegated(Locale.getDefault(), "app.locale")

    var logLevel by delegated(LogLevel.INFO.name, "app.log.level")

    val debugLevel by delegated(App.Debug.ECHO.name, "app.debug.level")

    var historyEnable by delegated(true, "app.history.enable")

    var historyLimits by delegated(DEFAULT_HISTORY_LIMITS, "app.history.limits")

    val supportedLocales by lazy {
        IOUtils.openResource("${RESOURCE_DIR}${I18N_DIR}/all.txt", javaClass.classLoader)?.bufferedReader()?.useLines {
            it.filter { it.isNotEmpty() && !it.startsWith('#') }.toList()
        } ?: emptyList<String>()
    }
}

object PluginSettings : Settings("${SETTINGS_DIR}/plugins") {
    override fun reset() {
        super.reset()
        comment = "Plugin settings"

        enable = enable
        blacklist = blacklist
    }

    var enable by delegated(true, "app.plugin.enable")

    var blacklist by delegated(App.pathOf("plugins/blacklist.lst"), "app.plugin.blacklist")
}

object UISettings : Settings("${SETTINGS_DIR}/ui") {
    override fun reset() {
        super.reset()
        comment = "UI components settings"

        lafTheme = lafTheme
        iconSets = iconSets
        globalFont = globalFont
        keyBindings = keyBindings
        antiAliasing = antiAliasing
        mnemonicEnable = mnemonicEnable
        windowDecorated = windowDecorated
    }

    var lafTheme by delegated(System.getProperty("ixin.theme") ?: DEFAULT_LAF_THEME, "ui.laf")

    var windowDecorated by delegated(false, "ui.laf.decorated")

    var globalFont: Font? get() = this["ui.font"] ?: Font.getFont("ixin.font")
        set(value) {
            if (value != null) {
                this["ui.font"] = value
            }
        }

    var antiAliasing by delegated(true, "ui.font.anti")

    var iconSets by delegated(System.getProperty("ixin.icons") ?: DEFAULT_ICON_SET, "ui.icons")

    var mnemonicEnable by delegated(Ixin.isMnemonicSupport, "ui.mnemonic.enable")

    var keyBindings by delegated(DEFAULT_KEY_BINDINGS, "ui.key.bindings")

    val supportedThemes by lazy {
        Ixin.themes.keys.sorted()
    }

    val supportedIcons by lazy {
        listOf(DEFAULT_ICON_SET)
    }
}
