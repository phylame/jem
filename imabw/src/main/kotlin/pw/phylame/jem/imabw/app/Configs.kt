/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
 *
 * This file is part of Imabw.
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

package pw.phylame.jem.imabw.app

import pw.phylame.qaf.core.Settings
import pw.phylame.qaf.core.fetchLanguages
import pw.phylame.qaf.ixin.Ixin
import pw.phylame.ycl.io.IOUtils
import pw.phylame.ycl.log.Level
import java.awt.Font
import java.util.*

object AppConfig : Settings("$SETTINGS_HOME/settings") {
    override fun reset() {
        super.reset()
        comment = "Common settings for Imabw"

        locale = locale
        pluginEnable = pluginEnable
        historyEnable = historyEnable
        historyLimits = historyLimits
    }

    var locale by delegated(Locale.getDefault(), "app.locale")

    var logLevel by delegated(Level.INFO.name, "app.log.level")

    var pluginEnable by delegated(true, "app.plugin.enable")

    var pluginBlacklist = emptySet<String>()
        set(value) {
        }

    var historyEnable by delegated(true, "app.history.enable")

    var historyLimits by delegated(DEFAULT_HISTORY_LIMITS, "app.history.limits")

    val supportedLocales by lazy {
        val url = IOUtils.resourceFor(RESOURCE_DIR + I18N_DIR + "all.txt", AppConfig.javaClass.classLoader)
        if (url != null) {
            fetchLanguages(url).map {
                Locale.forLanguageTag(it.replace('_', '-'))
            }
        } else {
            emptyList()
        }
    }
}

object UIConfig : Settings("$SETTINGS_HOME/ui") {
    override fun reset() {
        super.reset()
        comment = "UI components settings"
        lafTheme = lafTheme
        isWindowDecorated = isWindowDecorated
        globalFont = globalFont
        isAntiAliasing = isAntiAliasing
        iconSets = iconSets
        isMnemonicEnable = isMnemonicEnable
    }

    var lafTheme by delegated(System.getProperty("imabw.theme") ?: DEFAULT_LAF_THEME, "ui.laf")

    var isWindowDecorated by delegated(false, "ui.laf.decorated")

    var globalFont: Font? get() = this["ui.font"] ?: Font.getFont("imabw.font")
        set(value) {
            if (value != null) {
                this["ui.font"] = value
            }
        }

    var isAntiAliasing by delegated(true, "ui.font.anti")

    var iconSets by delegated(System.getProperty("imabw.icons") ?: DEFAULT_ICON_SET, "ui.icons")

    val isMnemonicSupport by lazy {
        "mac" !in System.getProperty("os.name")
    }

    var isMnemonicEnable by delegated(isMnemonicSupport, "ui.mnemonic.enable")

    val supportedThemes by lazy {
        Ixin.themes.keys.sorted()
    }

    val supportedIcons by lazy {
        listOf(DEFAULT_ICON_SET)
    }
}
