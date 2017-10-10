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

package jem.imabw

import javafx.application.Application
import jclp.log.Log
import jclp.log.LogLevel
import jem.Build
import jem.imabw.ui.Form
import mala.App
import mala.AppVerbose
import mala.ixin.IDelegate
import java.util.*
import java.util.concurrent.Executors

fun main(args: Array<String>) {
    App.run(Imabw, args)
}

object Imabw : IDelegate() {
    override val name = "imabw"

    override val version = Build.VERSION

    lateinit var form: Form // initialized by Form
        internal set

    override fun onStart() {
        Log.level = LogLevel.ALL
        App.verbose = AppVerbose.TRACE
        Locale.setDefault(Locale.ENGLISH)
        App.plugins.isEnable = true
        App.translator = App.assets.translatorFor("i18n/dev/app")
    }

    override fun run() {
        Application.launch(Form::class.java)
    }

    override fun onStop() {
        Workbench.dispose()
        Imabw.form.dispose()
        if (executor.isInitialized()) {
            executor.value.shutdown()
        }
    }

    override fun handle(command: String, source: Any): Boolean {
        when (command) {
            "gc" -> System.gc()
            else -> return super.handle(command, source)
        }
        return true
    }

    fun message(msg: String) {
        form.statusText = msg
    }

    // register for command handler
    fun register(handler: Any) {
        submit(Runnable { commandDispatcher.register(handler) })
    }

    // submit task in thread pool
    fun submit(r: Runnable) {
        executor.value.submit(r)
    }

    private val executor = lazy {
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    }
}
