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

import jclp.TranslatorWrapper
import jclp.log.Log
import jclp.text.or
import java.util.*

enum class AppState {
    DEFAULT,
    STARTING,
    RUNNING,
    STOPPING
}

enum class AppVerbose {
    NONE,
    ECHO,
    TRACE
}

interface AppDelegate : Runnable {
    val name: String

    val version: String

    fun onStart() {}

    fun onStop() {}

    fun restoreState(settings: AppSettings) {
        Log.level = settings.logLevel
        App.verbose = settings.appVerbose
        Locale.setDefault(settings.appLocale)
        App.translator = App.assets.translatorFor("i18n/$name")
        if (settings.enablePlugin) {
            with(App.plugins) {
                isEnable = true
                blacklist = settings.pluginBlacklist
            }
        }
    }

    fun saveState(settings: AppSettings) {
        settings.logLevel = Log.level
        settings.appVerbose = App.verbose
        settings.appLocale = Locale.getDefault()
        settings.enablePlugin = App.plugins.isEnable
    }
}

private typealias Cleanup = () -> Unit

object App : TranslatorWrapper() {
    var code: Int = 0
        private set

    @Volatile
    var state = AppState.DEFAULT
        private set

    var verbose = AppVerbose.ECHO

    lateinit var delegate: AppDelegate
        private set

    lateinit var arguments: Array<String>
        private set

    lateinit var thread: Thread
        private set

    val home by lazy {
        System.getProperty("mala.home") or { "${System.getProperty("user.home")}/.${delegate.name}" }
    }

    val assets by lazy {
        "!${delegate.javaClass.`package`?.name?.replace('.', '/').orEmpty()}/res".let {
            AssetManager(it, javaClass.classLoader)
        }
    }

    val plugins by lazy {
        PluginManager("META-INF/mala/plugin.lst", javaClass.classLoader)
    }

    fun run(delegate: AppDelegate, args: Array<String>) {
        thread = Thread.currentThread()
        this.delegate = delegate
        arguments = args
        onStart()
    }

    fun exit(code: Int = 0): Nothing {
        onQuit(code)
        System.exit(code)
        throw InternalError()
    }

    fun echo(msg: Any) {
        System.out.println("${delegate.name}: $msg")
    }

    fun error(msg: Any) {
        System.err.println("${delegate.name}: $msg")
    }

    fun error(msg: Any, e: Throwable) {
        error(msg, e, verbose)
    }

    fun error(msg: Any, e: Throwable, level: AppVerbose) {
        error(msg)
        traceback(e, level)
    }

    fun die(msg: Any): Nothing {
        error(msg)
        exit(-1)
    }

    fun die(msg: Any, e: Throwable): Nothing {
        error(msg, e)
        exit(-1)
    }

    fun die(msg: Any, e: Throwable, level: AppVerbose): Nothing {
        error(msg, e, level)
        exit(-1)
    }

    fun traceback(e: Throwable, level: AppVerbose) {
        when (level) {
            AppVerbose.ECHO -> System.err.println("\t${e.message}")
            AppVerbose.TRACE -> e.printStackTrace()
            else -> Unit
        }
    }

    private val cleanups = LinkedHashSet<Cleanup>()

    fun registerCleanup(action: Cleanup) {
        cleanups += action
    }

    fun removeCleanup(action: Cleanup) {
        cleanups -= action
    }

    private fun onStart() {
        state = AppState.STARTING
        delegate.onStart()
        plugins.init()
        state = AppState.RUNNING
        Runtime.getRuntime().addShutdownHook(Thread {
            if (state != AppState.STOPPING) {
                onQuit(-1)
            }
        })
        delegate.run()
    }

    private fun onQuit(code: Int) {
        if (state == AppState.RUNNING) {
            this.code = code
            state = AppState.STOPPING
            plugins.destroy()
            delegate.onStop()
            cleanups.forEach(Cleanup::invoke)
        }
    }
}
