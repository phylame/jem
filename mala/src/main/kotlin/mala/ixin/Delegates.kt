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

package mala.ixin

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.ToolBar
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import mala.App
import mala.AppDelegate

typealias MenuMap = MutableMap<String, Menu>

abstract class IDelegate : AppDelegate, CommandHandler {
    val menuMap = hashMapOf<String, Menu>()
    val actionMap = hashMapOf<String, Action>()
    val commandDispatcher = CommandDispatcher()

    fun getAction(id: String) = actionMap[id]

    fun newAction(id: String) = actionMap.getOrCreate(id, App, App.assets)

    override fun handle(command: String, source: Any): Boolean {
        if (!commandDispatcher.handle(command, source)) {
            App.error("No handler for command: '$command'")
            return false
        }
        return true
    }
}

class AppPane : BorderPane() {
    val menuBarProperty = SimpleObjectProperty<MenuBar>()
    val toolBarProperty = SimpleObjectProperty<ToolBar>()
    val statusBarProperty = SimpleObjectProperty<StatusBar>()

    var menuBar by menuBarProperty
    var toolBar by toolBarProperty
    var statusBar by statusBarProperty

    private val topBox = VBox()

    init {
        styleClass += "app-pane"
        val visibleListener = ChangeListener<Boolean> { observable, _, visible ->
            val node = (observable as BooleanProperty).bean as Node
            if (node is StatusBar) {
                bottom = if (visible) node else null
            } else {
                if (visible) {
                    topBox.children += node
                } else {
                    topBox.children.remove(node)
                }
            }
        }
        menuBarProperty.addListener { _, oldBar, newBar ->
            if (oldBar !== newBar) {
                val items = topBox.children
                if (oldBar == null) { // add menu bar
                    if (items.isEmpty()) {
                        items += newBar
                    } else {
                        items.add(0, newBar)
                    }
                    newBar.visibleProperty().addListener(visibleListener)
                } else if (newBar == null) { // remove menu bar
                    if (items.first() is MenuBar) {
                        items.remove(oldBar)
                        oldBar.visibleProperty().removeListener(visibleListener)
                    }
                } else { // replace menu bar
                    if (items.first() is MenuBar) {
                        items[0] = newBar
                        newBar.visibleProperty().addListener(visibleListener)
                        oldBar.visibleProperty().removeListener(visibleListener)
                    }
                }
                if (items.isEmpty()) {
                    top = null
                } else if (top == null) {
                    top = topBox
                }
            }
        }
        toolBarProperty.addListener { _, oldBar, newBar ->
            if (oldBar !== newBar) {
                val items = topBox.children
                if (oldBar == null) { // add tool bar
                    if (items.isEmpty()) {
                        items += newBar
                    } else {
                        items.add(1, newBar)
                    }
                    newBar.visibleProperty().addListener(visibleListener)
                } else if (newBar == null) { // remove tool bar
                    if (items.last() is ToolBar) {
                        items.remove(oldBar)
                        oldBar.visibleProperty().removeListener(visibleListener)
                    }
                } else { // replace tool bar
                    if (items.last() is ToolBar) {
                        items[items.size - 1] = newBar
                        newBar.visibleProperty().addListener(visibleListener)
                        oldBar.visibleProperty().removeListener(visibleListener)
                    }
                }
                if (items.isEmpty()) {
                    top = null
                } else if (top == null) {
                    top = topBox
                }
            }
        }
        statusBarProperty.addListener { _, oldBar, newBar ->
            if (oldBar !== newBar) {
                bottom = newBar
                newBar?.visibleProperty()?.addListener(visibleListener)
                oldBar?.visibleProperty()?.removeListener(visibleListener)
            }
        }
    }

    fun setupMenuBar(items: Collection<Item>) {
        if (items.isNotEmpty()) {
            menuBar = MenuBar().apply {
                val menus = this.menus
                styleClass += "app-menu-bar"
                val delegate = App.delegate as IDelegate
                items.filterIsInstance<ItemGroup>().forEach {
                    menus += it.toMenu(delegate, App, App.assets, delegate.actionMap, delegate.menuMap)
                }
            }
        }
    }

    fun setupToolBar(items: Collection<*>) {
        if (items.isNotEmpty()) {
            toolBar = ToolBar().apply {
                styleClass += "app-tool-bar"
                val delegate = App.delegate as IDelegate
                init(items, delegate, App, App.assets, delegate.actionMap)
            }
        }
    }

    fun setupStatusBar() {
        statusBar = StatusBar()
    }

    fun setup(designer: AppDesigner) {
        designer.items["menuBar"]?.let(this::setupMenuBar)
        designer.items["toolBar"]?.let(this::setupToolBar)
        setupStatusBar()
    }
}

class StatusBar : BorderPane() {
    var text: String
        get() = statusLabel.text
        set(value) {
            statusLabel.text = value
        }

    val statusLabel = Label()

    init {
        styleClass += "app-status-bar"

        left = statusLabel.also {
            styleClass += "app-status-text"
            BorderPane.setAlignment(it, Pos.CENTER)
        }
    }
}
