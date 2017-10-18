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
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem
import jclp.io.createRecursively
import jclp.setting.delegate
import jem.epm.ParserParam
import mala.App
import mala.AppSettings
import mala.MalaSettings
import mala.ixin.IxIn
import mala.ixin.IxInSettings
import java.io.File

object GeneralSettings : AppSettings() {
    var enableHistory by delegate(true, "app.history.enable")

    var historyLimit by delegate(24, "app.history.limit")
}

object UISettings : IxInSettings() {
    var navigationBarVisible by delegate(true, "form.navigationBar.visible")
}

object EditorSettings : MalaSettings("config/editor.ini") {
    var wrapText by delegate(false, "editor.wrapText")

    var showLineNumber by delegate(true, "editor.showLineNumber")
}

object History {
    private val file = File(App.home, "config/history.txt")

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
                        items.add(0, MenuItem(path).apply {
                            setOnAction { Workbench.openBook(ParserParam(text)) }
                        })
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
            if (file.exists()) {
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
            if (file.exists() || file.parentFile.createRecursively()) {
                file.bufferedWriter().use { out ->
                    paths.forEach { out.append(it).append("\n") }
                }
            }
        }
    }
}
