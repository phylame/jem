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

package mala.ixin

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.MenuBar
import javafx.scene.control.ToolBar
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import mala.App

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

class AppPane : BorderPane() {
    private val menuBarProperty = SimpleObjectProperty<MenuBar>()
    private val toolBarProperty = SimpleObjectProperty<ToolBar>()
    private val statusBarProperty = SimpleObjectProperty<StatusBar>()

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
                oldBar?.visibleProperty()?.removeListener(visibleListener)
                newBar?.visibleProperty()?.addListener(visibleListener)
                val items = topBox.children
                if (oldBar == null) { // add menu bar
                    if (items.isEmpty()) {
                        items += newBar
                    } else {
                        items.add(0, newBar)
                    }
                } else if (newBar == null) { // remove menu bar
                    if (items.first() is MenuBar) {
                        items.remove(oldBar)
                    }
                } else { // replace menu bar
                    if (items.first() is MenuBar) {
                        items[0] = newBar
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
                oldBar?.visibleProperty()?.removeListener(visibleListener)
                newBar?.visibleProperty()?.addListener(visibleListener)
                val items = topBox.children
                if (oldBar == null) { // add tool bar
                    if (items.isEmpty()) {
                        items += newBar
                    } else {
                        items.add(1, newBar)
                    }
                } else if (newBar == null) { // remove tool bar
                    if (items.last() is ToolBar) {
                        items.remove(oldBar)
                    }
                } else { // replace tool bar
                    if (items.last() is ToolBar) {
                        items[items.size - 1] = newBar
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

    fun setupMenuBar(items: Collection<Item>, actionMap: ActionMap, menuMap: MenuMap) {
        if (items.isNotEmpty()) {
            menuBar = MenuBar().apply {
                val menus = this.menus
                styleClass += "app-menu-bar"
                items.filterIsInstance<ItemGroup>().forEach {
                    menus += it.toMenu(IxIn.delegate, App, App.assets, actionMap, menuMap)
                }
            }
        }
    }

    fun setupToolBar(items: Collection<*>, actionMap: ActionMap) {
        if (items.isNotEmpty()) {
            toolBar = ToolBar().apply {
                styleClass += "app-tool-bar"
                init(items, IxIn.delegate, App, App.assets, actionMap)
            }
        }
    }

    fun setupStatusBar() {
        statusBar = StatusBar()
    }

    fun setup(designer: AppDesigner, actionMap: ActionMap, menuMap: MenuMap) {
        designer.items["menuBar"]?.let { setupMenuBar(it, actionMap, menuMap) }
        designer.items["toolBar"]?.let { setupToolBar(it, actionMap) }
        setupStatusBar()
    }
}
