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
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.stage.Stage
import jem.Build
import jem.imabw.ui.Form
import mala.App
import mala.ixin.*
import java.util.*
import java.util.concurrent.Executors

fun main(args: Array<String>) {
    App.run(Imabw, args)
}

object Imabw : IDelegate() {
    override val name = "imabw"

    override val version = Build.VERSION

    val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

    override fun onStart() {
        Locale.setDefault(Locale.ENGLISH)
        App.translator = App.assets.translatorFor("i18n/dev/app")
    }

    override fun run() {
        Application.launch(Form::class.java)
    }

    override fun onStop() {
        executor.shutdown()
    }

    override fun handle(command: String, source: Any) {
        println("command = [$command], source = [$source]")
        if (command == "help") {
            actionMap["exit"]?.isDisable = true
        }
    }
}

class UI : Application() {
    override fun start(stage: Stage) {
        stage.title = "Imabw"

        val status = Label("Ready")
        status.padding = Insets(4.0, 4.0, 4.0, 4.0)

        val root = AppPane()

        root.setup(App.assets.designerFor("ui/actions.json")!!)
        root.center = SplitPane().also {
            it.items += BorderPane().also { box ->
                box.top = HBox().also {
                    it += Label("Contents", App.assets.graphicFor("tree/contents"))
                }
                box.center = TreeView<String>().also {
                    it.root = TreeItem("Root").also {
                        for (i in 1..36) {
                            it.children += TreeItem("Chapter $i")
                        }
                    }
                }

            }
            it.items += TabPane().also {
                for (i in 1..10) {
                    it.tabs += Tab("Chapter $i", TextArea())
                }
            }
        }

        root.statusBar = HBox().also {
            it += status
        }

        stage.scene = Scene(root).also {
            //            it.stylesheets.add(App.assets.resourceFor("ui/main.css")?.toExternalForm())
        }
        stage.show()
    }
}
