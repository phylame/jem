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

import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.scene.Node
import javafx.scene.control.MenuBar
import javafx.scene.control.ToolBar
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import mala.App
import mala.AppDelegate

abstract class IDelegate : AppDelegate, CommandHandler {
    val actions = FXCollections.observableHashMap<String, Action>()
}

class AppPane : BorderPane() {
    val menuBarProperty = SimpleObjectProperty<MenuBar>()
    val toolBarProperty = SimpleObjectProperty<ToolBar>()
    val statusBarProperty = SimpleObjectProperty<Node>()

    var menuBar by menuBarProperty
    var toolBar by toolBarProperty
    var statusBar by statusBarProperty

    private val topBox = VBox()

    init {
        menuBarProperty.addListener { _, oldValue, newValue ->
            if (oldValue !== newValue) {
                val items = topBox.children
                if (oldValue == null) { // add menu bar
                    if (items.isEmpty()) {
                        items += newValue
                    } else {
                        items.add(0, newValue)
                    }
                } else if (newValue == null) { // remove menu bar
                    if (items.first() is MenuBar) {
                        items.remove(oldValue)
                    }
                } else { // replace menu bar
                    if (items.first() is MenuBar) {
                        items[0] = newValue
                    }
                }
                if (items.isEmpty()) {
                    top = null
                } else if (top == null) {
                    top = topBox
                }
            }
        }
        toolBarProperty.addListener { _, oldValue, newValue ->
            if (oldValue !== newValue) {
                val items = topBox.children
                if (oldValue == null) { // add tool bar
                    if (items.isEmpty()) {
                        items += newValue
                    } else {
                        items.add(1, newValue)
                    }
                } else if (newValue == null) { // remove tool bar
                    if (items.last() is ToolBar) {
                        items.remove(oldValue)
                    }
                } else { // replace tool bar
                    if (items.last() is ToolBar) {
                        items[items.size - 1] = newValue
                    }
                }
                if (items.isEmpty()) {
                    top = null
                } else if (top == null) {
                    top = topBox
                }
            }
        }
        statusBarProperty.addListener { _, oldValue, newValue ->
            if (oldValue !== newValue) {
                bottom = newValue
            }
        }
    }

    fun setupMenuBar(groups: Collection<Item>) {
        if (groups.isNotEmpty()) {
            val bar = MenuBar()
            val menus = bar.menus
            val delegate = App.delegate as IDelegate
            groups.filterIsInstance<ItemGroup>().forEach {
                menus += it.toMenu(delegate, App, App.assets, delegate.actions)
            }
            menuBar = bar
        }
    }

    fun setupToolBar(items: Collection<*>) {
        toolBar = ToolBar().apply {
            val delegate = App.delegate as IDelegate
            init(items, delegate, App, App.assets, delegate.actions)
        }
    }

    fun setup(designer: AppDesigner) {
        designer.menuBar?.let(this::setupMenuBar)
        designer.toolBar?.let(this::setupToolBar)
    }
}
