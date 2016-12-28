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

import pw.phylame.jem.epm.EpmManager
import pw.phylame.jem.imabw.app.ui.Dialogs
import pw.phylame.jem.imabw.app.ui.Viewer
import pw.phylame.qaf.core.App
import pw.phylame.qaf.core.lines
import pw.phylame.qaf.core.tr
import pw.phylame.qaf.ixin.Command
import pw.phylame.qaf.ixin.CommandDispatcher
import pw.phylame.qaf.ixin.IDelegate
import pw.phylame.qaf.ixin.Ixin
import pw.phylame.qaf.ixin.Resource
import pw.phylame.ycl.io.IOUtils
import pw.phylame.ycl.log.Log
import pw.phylame.ycl.log.LogLevel
import java.util.Locale

object Imabw : IDelegate<Viewer>() {
    override fun onStart() {
        super.onStart()
        App.ensureHomeExisted()
        System.setProperty(EpmManager.AUTO_LOAD_CUSTOMIZED_KEY, "true")
        proxy = CommandDispatcher(arrayOf(this))

        Log.setLevel(LogLevel.forName(AppSettings.logLevel, LogLevel.INFO))
        App.debug = App.Debug.valueOf(AppSettings.debugLevel)
        Locale.setDefault(AppSettings.appLocale)
        resource = Resource(RESOURCE_DIR, "$IMAGE_DIR/${UISettings.iconSets}", I18N_DIR, Imabw.javaClass.classLoader)
        App.translator = resource.translatorFor(TRANSLATOR_NAME)

        if (PluginSettings.enable) {
            val blacklist = if (PluginSettings.blacklist.isNotBlank())
                IOUtils.resourceFor(PluginSettings.blacklist, Imabw.javaClass.classLoader)
                        ?.lines()
                        ?.toSet()
                        ?: emptySet<String>()
            else emptySet<String>()
            App.loadPlugins(blacklist)
        }

        addProxy(Manager)
    }

    override fun createForm(): Viewer {
        // init global swing environment
        Ixin.isMnemonicEnable = UISettings.mnemonicEnable
        Ixin.init(UISettings.antiAliasing, UISettings.windowDecorated, UISettings.lafTheme, UISettings.globalFont)
        // create viewer
        val viewer = Viewer()
        viewer.statusText = tr("viewer.status.ready")
        viewer.isVisible = true
        return viewer
    }

    fun addProxy(proxy: Any) {
        (this.proxy as CommandDispatcher).addProxy(proxy)
    }

    override fun onReady() {
        Manager.newFile(tr("d.newBook.defaultTitle"))
    }

    fun message(id: String) {
        form.statusText = tr(id)
    }

    fun message(id: String, vararg args: Any) {
        form.statusText = tr(id, args)
    }

    @Command(EDIT_SETTINGS)
    fun editSettings() {
        Dialogs.editSettings(Imabw.form)
    }

    @Command(HELP_CONTENTS)
    fun showHelp() {
        Dialogs.browse(DOCUMENT_URL)
    }

    @Command(ABOUT_APP)
    fun aboutApp() {
        Dialogs.showAbout(Imabw.form)
    }
}

fun main(args: Array<String>) {
    App.run(APP_NAME, APP_VERSION, args, Imabw)
}
