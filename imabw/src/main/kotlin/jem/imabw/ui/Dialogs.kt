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

package jem.imabw.ui

import javafx.scene.control.TextInputDialog
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Window
import jclp.VariantMap
import jem.Chapter
import jem.epm.EpmManager
import jem.imabw.Imabw
import mala.App
import java.io.File

inline fun inputText(title: String, tip: String, text: String, block: (String) -> Unit) {
    with(TextInputDialog(text)) {
        graphic = null
        headerText = null
        this.title = title
        this.contentText = tip
        initOwner(Imabw.form.stage)
        initModality(Modality.WINDOW_MODAL)
        showAndWait().let { if (it.isPresent) block(it.get()) }
    }
}

private val fileChooser = FileChooser()

fun openBooks(owner: Window): List<File> {
    fileChooser.title = App.tr("d.openBook.title")
    val filters = fileChooser.extensionFilters.apply { clear() }
    EpmManager.services.filter { it.hasParser && "crawler" !in it.keys }.map { factory ->
        val name = factory.keys.first().let {
            App.optTr("misc.ext.$it") ?: App.tr("misc.ext.common", it.toUpperCase())
        }
        FileChooser.ExtensionFilter(name, factory.keys.map { "*.$it" }).apply {
            if ("pmab" in factory.keys) {
                fileChooser.selectedExtensionFilter = this
            }
        }
    }.toCollection(filters)
    return fileChooser.showOpenMultipleDialog(owner)?.also {
        fileChooser.initialDirectory = it.first().parentFile
    } ?: emptyList()
}

fun editAttributes(chapter: Chapter) {
    println("edit attributes of $chapter")
}

fun editVariants(map: VariantMap, title: String) {
    println("edit variants for $title: $map")
}
