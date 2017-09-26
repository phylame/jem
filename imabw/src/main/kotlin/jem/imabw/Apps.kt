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
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage
import jclp.io.getResource

fun main(args: Array<String>) {
    Application.launch(Imabw::class.java, *args)
}

class Imabw : Application() {
    override fun start(stage: Stage) {
        stage.title = "Imabw"
        val root = VBox()

        val menuBar = MenuBar()

        menuBar.menus.addAll(Menu("File").apply {
            items += MenuItem("New")
            items += MenuItem("Open")
            items += SeparatorMenuItem()
            items += MenuItem("Save")
        }, Menu("Edit").apply {
            items += MenuItem("Undo")
            items += MenuItem("Redo")
            items += SeparatorMenuItem()
            items += MenuItem("Cut")
            items += MenuItem("Copy")
            items += MenuItem("Paste")
        }, Menu("View").apply {

        })

        root.children.add(menuBar)

        SplitPane().apply {
            val tree = TreeView<String>()
            items += ScrollPane(tree)

            val area = TextArea()
            items += ScrollPane(area)

            root.children += this
        }

        HBox().apply {
            children += Label("Ready")
            root.children += this
        }

        val scene = Scene(root, 400.0, 350.0)
        scene.stylesheets.add(getResource("!jem/imabw/res/style.css")?.toExternalForm())
        stage.scene = scene
        stage.show()
    }
}
