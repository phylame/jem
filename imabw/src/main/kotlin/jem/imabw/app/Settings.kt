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

import jclp.log.Level
import qaf.core.App
import qaf.core.Settings
import qaf.core.Verbose
import qaf.ixin.Ixin
import qaf.ixin.fileFor
import java.awt.Color
import java.awt.Font
import java.util.*
import javax.swing.JTabbedPane

object AppSettings : Settings("$SETTINGS_DIR/settings") {
    override fun reset() {
        super.reset()
        comment = "Common settings for Imabw"

        appLocale = appLocale
        logLevel = logLevel
        isHistoryEnable = isHistoryEnable
        historyLimits = historyLimits
    }

    var appLocale: Locale by delegated(Locale.getDefault(), "app.locale")

    var logLevel by delegated(Level.INFO.name, "app.log.level")

    val debugLevel by delegated(Verbose.ECHO.name, "app.debug.level")

    var isHistoryEnable by delegated(true, "app.history.enable")

    var historyLimits by delegated(DEFAULT_HISTORY_LIMITS, "app.history.limits")

    val supportedLocales by lazy {
        fileFor("$I18N_DIR/all.txt")
                ?.openStream()
                ?.bufferedReader()
                ?.lineSequence()
                ?.filter { it.isNotEmpty() && !it.startsWith('#') }
                ?.toList()
                ?: emptyList<String>()
    }
}

object PluginSettings : Settings("$SETTINGS_DIR/plugins") {
    override fun reset() {
        super.reset()
        comment = "Plugin settings for Imabw"

        isEnable = isEnable
        blacklist = blacklist
    }

    var isEnable by delegated(true, "app.plugin.enable")

    var blacklist by delegated(App.pathOf("plugins/blacklist.lst"), "app.plugin.blacklist")
}

object UISettings : Settings("$SETTINGS_DIR/ui") {
    override fun reset() {
        super.reset()
        comment = "UI settings for Imabw"

        lafTheme = lafTheme
        iconSets = iconSets
        globalFont = globalFont
        keyBindings = keyBindings
        isAntiAliasing = isAntiAliasing
        isMnemonicEnable = isMnemonicEnable
        isWindowDecorated = isWindowDecorated
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

    var isMnemonicEnable by delegated(Ixin.isMnemonicSupport, "ui.mnemonic.enable")

    var keyBindings by delegated(DEFAULT_KEY_BINDINGS, "ui.key.bindings")

    val supportedThemes by lazy {
        Ixin.themes.keys.sorted()
    }

    val supportedIcons by lazy {
        listOf(DEFAULT_ICON_SET)
    }
}

object EditorSettings : Settings("$SETTINGS_DIR/editor") {
    override fun reset() {
        super.reset()
        comment = "Text editor settings for Imabw"

        font = null
        background = background
        foreground = foreground
        highlight = highlight
        isLineWrap = isLineWrap
        isWordWrap = isWordWrap
        isLineNumberVisible = isLineNumberVisible
        tabPlacement = tabPlacement
        tabLayout = tabLayout
    }

    var font: Font? get() = get("editor.font", null, Font::class.java)
        set(value) {
            if (value != null) {
                set("editor.font", value)
            } else {
                set("editor.font", "")
            }
        }

    var background by delegated(Color.WHITE, "editor.background")

    var foreground by delegated(Color.BLACK, "editor.foreground")

    var highlight by delegated(Color.BLACK, "editor.highlight")

    var isLineWrap by delegated(true, "editor.lineWrap")

    var isWordWrap by delegated(true, "editor.wordWrap")

    var isLineNumberVisible by delegated(false, "editor.showLineNumber")

    var tabPlacement by delegated(JTabbedPane.TOP, "editor.tab.placement")

    var tabLayout by delegated(JTabbedPane.SCROLL_TAB_LAYOUT, "editor.tab.layout")
}
