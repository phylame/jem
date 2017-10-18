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

package jem.imabw

import javafx.application.Application
import jem.Build
import jem.imabw.ui.Dashboard
import mala.App
import mala.ixin.IDelegate
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future

fun main(args: Array<String>) {
    App.run(Imabw, args)
}

object Imabw : IDelegate() {
    override val name = "imabw"

    override val version = Build.VERSION

    val dashboard get() = fxApp as Dashboard

    override fun onStart() {
//        Log.facade = SLF4JFacade
        submitAndWait({ GeneralSettings }, { EditorSettings }, { UISettings })
        restoreState(GeneralSettings)
    }

    override fun run() {
        Application.launch(Dashboard::class.java)
    }

    override fun onReady() {
        Workbench.start()
    }

    override fun onStop() {
        saveState(GeneralSettings)

        dashboard.dispose()
        Workbench.dispose()

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
        fxApp.statusText = msg
    }

    fun register(handler: Any) {
        submit { commandProxy.register(handler) }
    }

    fun submit(r: Runnable): Future<*> = executor.value.submit(r)

    fun <V> submit(r: Callable<V>): Future<V> = executor.value.submit(r)

    fun <V> submit(block: () -> V): Future<V> = executor.value.submit(block)

    fun submitAndWait(tasks: Collection<Runnable>) {
        if (tasks.isEmpty()) return
        val latch = CountDownLatch(tasks.size)
        for (task in tasks) {
            submit {
                task.run()
                latch.countDown()
            }
        }
        latch.await()
    }

    fun submitAndWait(vararg tasks: () -> Unit) {
        if (tasks.isEmpty()) return
        val latch = CountDownLatch(tasks.size)
        for (task in tasks) {
            submit {
                task()
                latch.countDown()
            }
        }
        latch.await()
    }

    private val executor = lazy {
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1)
    }
}
