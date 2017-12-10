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

import javafx.beans.binding.Bindings
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.scene.control.Dialog
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.control.TableView
import jclp.io.exists
import jclp.io.notExists
import jclp.setting.getDouble
import jclp.setting.getString
import jclp.setting.settingsWith
import mala.App
import mala.AppSettings
import mala.MalaSettings
import mala.ixin.IxIn
import mala.ixin.IxInSettings
import java.nio.file.Files
import java.nio.file.Paths

object GeneralSettings : AppSettings() {
    var enableHistory by settingsWith(true, "app.history.enable")

    var historyLimit by settingsWith(24, "app.history.limit")

    var lastBookDir by settingsWith("", "app.history.bookDir")

    var lastImageDir by settingsWith("", "app.history.imageDir")
}

object UISettings : IxInSettings() {
    var stylesheetUri by settingsWith("", "ui.stylesheet.uri")

    var navigationBarVisible by settingsWith(true, "form.navigationBar.visible")

    fun restore(dialog: Dialog<*>, tag: String) {
        dialog.dialogPane.apply {
            getDouble("dialog.$tag.width")?.let { prefWidth = it }
            getDouble("dialog.$tag.height")?.let { prefHeight = it }
        }
        getDouble("dialog.$tag.x")?.let { dialog.x = it }
        getDouble("dialog.$tag.y")?.let { dialog.y = it }
    }

    fun store(dialog: Dialog<*>, tag: String) {
        set("dialog.$tag.width", dialog.dialogPane.width)
        set("dialog.$tag.height", dialog.dialogPane.height)
        set("dialog.$tag.x", dialog.x)
        set("dialog.$tag.y", dialog.y)
    }

    fun restore(table: TableView<*>, tag: String) {
        table.columns.forEachIndexed { index, column ->
            getDouble("table.$tag.column.$index")?.let { column.prefWidth = it }
        }
    }

    fun store(table: TableView<*>, tag: String) {
        table.columns.forEachIndexed { index, column ->
            set("table.$tag.column.$index", column.width)
        }
    }
}

object EditorSettings : MalaSettings("config/editor.ini") {
    var wrapText by settingsWith(false, "editor.wrapText")

    var showLineNumber by settingsWith(true, "editor.showLineNumber")
}

object JemSettings : MalaSettings("config/jem.ini") {
    var genres by settingsWith("", "jem.values.genres")

    var states by settingsWith(App.tr("jem.value.states"), "jem.values.states")

    var multipleAttrs by settingsWith("author;keywords;vendor;tag;subject;protagonist", "jem.values.multiple")

    fun getValue(name: String) = getString("jem.values.$name")
}

object History {
    private val file = Paths.get(App.home, "config/history.txt")

    private val paths = FXCollections.observableArrayList<String>()

    val latest get() = paths.firstOrNull()

    private var isModified = false

    init {
        IxIn.actionMap["clearHistory"]?.disableProperty?.bind(Bindings.isEmpty(paths))
        paths.addListener(ListChangeListener {
            val items = IxIn.menuMap["menuHistory"]!!.items
            val isEmpty = items.size == 1
            while (it.next()) {
                if (it.wasRemoved()) {
                    for (path in it.removed) {
                        items.removeIf { it.text == path }
                    }
                    if (items.size == 2) { // remove separator
                        items.remove(0, 1)
                    }
                }
                if (it.wasAdded()) {
                    val paths = items.map { it.text }
                    it.addedSubList.filter { it !in paths }.asReversed().mapIndexed { i, path ->
                        if (i == 0 && isEmpty) { // insert separator
                            items.add(0, SeparatorMenuItem())
                        }
                        items.add(0, MenuItem(path).apply { setOnAction { Workbench.openFile(text) } })
                    }
                }
            }
        })
        load()
    }

    fun remove(path: String) {
        if (GeneralSettings.enableHistory) {
            paths.remove(path)
            isModified = true
        }
    }

    fun insert(path: String) {
        if (GeneralSettings.enableHistory) {
            paths.remove(path)
            if (paths.size == GeneralSettings.historyLimit) {
                paths.remove(paths.size - 1, paths.size)
            }
            paths.add(0, path)
            isModified = true
        }
    }

    fun clear() {
        if (GeneralSettings.enableHistory) {
            paths.clear()
            isModified = true
        }
    }

    fun load() {
        if (GeneralSettings.enableHistory) {
            if (file.exists) {
                with(ReadLineTask(file)) {
                    setOnSucceeded {
                        paths += value
                        hideProgress()
                    }
                    Imabw.submit(this)
                }
            }
        }
    }

    fun sync() {
        if (GeneralSettings.enableHistory && isModified) {
            if (file.notExists) {
                try {
                    Files.createDirectories(file.parent)
                } catch (e: Exception) {
                    App.error("cannot create directory for history", e)
                    return
                }
            }
            Files.write(file, paths)
        }
    }
}
