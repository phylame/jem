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

import pw.phylame.jem.imabw.app.ui.Performer
import pw.phylame.qaf.core.App
import pw.phylame.qaf.core.tr
import pw.phylame.qaf.ixin.*
import pw.phylame.ycl.log.Level
import pw.phylame.ycl.log.Log
import java.awt.Desktop
import java.net.URI
import java.util.*

object Imabw : IxinDelegate<Performer>() {
    override fun onStart() {
        super.onStart()
        Log.setLevel(Level.forName(AppConfig.logLevel, Level.INFO))
        App.ensureHomeExisted()
        Locale.setDefault(AppConfig.locale)
        resource = Resource(RESOURCE_DIR, IMAGE_DIR + '/' + UIConfig.iconSets, I18N_DIR, Imabw.javaClass.classLoader)
        App.translator = resource.translatorFor(I18N_NAME)
        if (AppConfig.pluginEnable) {
            App.loadPlugins(AppConfig.pluginBlacklist)
        }
    }

    override fun createForm(): Performer {
        Ixin.mnemonicEnable = UIConfig.isMnemonicEnable
        Ixin.setAntiAliasing(UIConfig.isAntiAliasing)
        if (UIConfig.isWindowDecorated) {
            Ixin.setWindowDecorated(true)
        }
        Ixin.setLafTheme(UIConfig.lafTheme)
        if (UIConfig.globalFont != null) {
            Ixin.setGlobalFont(UIConfig.globalFont!!)
        }
        proxy = CommandDispatcher(arrayOf(this, Manager, Performer))
        Performer.statusText = tr("viewer.status.ready")
        return Performer
    }

    @Command(HELP_CONTENTS)
    fun showHelp() {
        Desktop.getDesktop().browse(URI(DOCUMENT))
    }

    @Command
    fun aboutApp() {

    }
}

fun main(args: Array<String>) {
    App.run(NAME, VERSION, args, Imabw)
}
