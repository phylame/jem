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

import jclp.TranslatorWrapper
import jclp.io.createRecursively
import jclp.text.or
import java.io.File

private typealias Cleanup = () -> Unit

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
}

object App : TranslatorWrapper() {
    private const val APP_HOME_KEY = "mala.home"
    private const val PLUGIN_REGISTRY_PATH = "META-INF/mala/plugin.prop"

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

    val assets by lazy { AssetManager(home, javaClass.classLoader) }

    val appAssets by lazy { AssetManager(appHome, javaClass.classLoader) }

    val plugins by lazy { PluginManager(PLUGIN_REGISTRY_PATH, javaClass.classLoader) }

    val home by lazy { "!${delegate.javaClass.`package`?.name?.replace('.', '/').orEmpty()}/res" }

    val appHome by lazy { System.getProperty(APP_HOME_KEY) or { "${System.getProperty("user.home")}/.${delegate.name}" } }

    fun initAppHome() = File(appHome).createRecursively()

    fun resetAppHome() = File(appHome).deleteRecursively()

    fun run(delegate: AppDelegate, args: Array<String>) {
        this.delegate = delegate
        arguments = args
        onStart()
    }

    fun exit(code: Int = 0): Nothing {
        onQuit(code)
        System.exit(code)
        throw InternalError()
    }

    private fun onStart() {
        state = AppState.STARTING
        delegate.onStart()
        plugins.init()
        state = AppState.RUNNING
        delegate.run()
        Runtime.getRuntime().addShutdownHook(Thread({
            if (state != AppState.STOPPING) {
                onQuit(-1)
            }
        }))
    }

    private fun onQuit(code: Int) {
        this.code = code
        state = AppState.STOPPING
        plugins.destroy()
        delegate.onStop()
        cleanups.forEach(Cleanup::invoke)
    }

    fun echo(msg: Any) {
        System.out.println("${delegate.name}: $msg")
    }

    fun error(msg: Any) {
        System.err.println("${delegate.name}: $msg")
    }

    fun error(msg: Any, e: Throwable) {
        error(msg)
        traceback(e, verbose)
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

    operator fun plusAssign(action: Cleanup) {
        cleanups += action
    }

    operator fun minusAssign(action: Cleanup) {
        cleanups -= action
    }
}
