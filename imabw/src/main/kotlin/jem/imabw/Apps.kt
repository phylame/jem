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
import javafx.scene.Scene
import javafx.scene.control.MenuBar
import javafx.scene.control.ToolBar
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import jclp.io.openResource
import jem.Build
import mala.App
import mala.ixin.*
import org.json.JSONObject
import org.json.JSONTokener
import java.util.*

fun main(args: Array<String>) {
    App.run(Imabw, args)
}

object Imabw : IDelegate() {
    override val name = "imabw"

    override val version = Build.VERSION

    override fun onStart() {
        Locale.setDefault(Locale.ENGLISH)
        App.translator = App.assets.translatorFor("i18n/dev/app")
    }

    override fun run() {
        Application.launch(UI::class.java)
    }
}

class UI : Application() {
    override fun start(stage: Stage) {
        stage.title = "Imabw"

        val h = object : CommandHandler {
            override fun handle(command: String, source: Any) {
                println("command = [${command}], source = [${source}]")
            }
        }

        val json = JSONObject(JSONTokener(openResource("!jem/imabw/res/ui/actions.json")))
        val items = LinkedList<Item>()
        parseItems(json.optJSONArray("menubar"), items)
        val menuBar = MenuBar()
        for (item in items) {
            if (item is ItemGroup) {
                menuBar.menus += item.toMenu(h, App, App.assets)
            }
        }

        val toolBar = ToolBar()
        items.clear()
        parseItems(json.optJSONArray("toolbar"), items)
        toolBar.init(items, h, App, App.assets)

        val root = BorderPane()

        root.top = VBox().also {
            it += menuBar
            it += toolBar
        }

        stage.scene = Scene(root)

        stage.show()
    }
}
