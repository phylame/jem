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

package jem.imabw.ui

import javafx.scene.control.*
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Window
import jclp.VariantMap
import jclp.traceText
import jem.Chapter
import jem.epm.EpmManager
import jem.epm.PMAB_NAME
import jem.imabw.History
import jem.imabw.Imabw
import mala.App
import mala.ixin.graphicFor
import java.io.File

private fun Dialog<*>.init(title: String, owner: Window) {
    headerText = null
    this.title = title
    initModality(Modality.WINDOW_MODAL)
    initOwner(owner)
}

fun alert(type: Alert.AlertType, title: String, content: String, owner: Window = Imabw.fxApp.stage): Alert {
    return Alert(type, content).apply { init(title, owner) }
}

fun info(title: String, content: String, owner: Window = Imabw.fxApp.stage): Alert {
    return alert(Alert.AlertType.INFORMATION, title, content, owner)
}

fun error(title: String, content: String, owner: Window = Imabw.fxApp.stage) {
    alert(Alert.AlertType.ERROR, title, content, owner).showAndWait()
}

fun confirm(title: String, content: String, owner: Window = Imabw.fxApp.stage): Alert {
    return alert(Alert.AlertType.CONFIRMATION, title, content, owner)
}

fun traceback(title: String, content: String, throwable: Throwable, owner: Window = Imabw.fxApp.stage) {
    with(alert(Alert.AlertType.NONE, title, content, owner)) {
        dialogPane.expandableContent = TextArea().apply {
            isEditable = false
            isFocusTraversable = false
            styleClass += "dialog-content"
        }
        dialogPane.buttonTypes += ButtonType.CLOSE
        graphic = App.assets.graphicFor("dialog/bug")
        dialogPane.expandedProperty().addListener { _ ->
            (dialogPane.expandableContent as TextArea).let { if (it.text.isEmpty()) it.text = throwable.traceText }
        }
        showAndWait()
    }
}

fun inputText(title: String, content: String, text: String, owner: Window = Imabw.fxApp.stage): String? {
    return TextInputDialog(text).run {
        graphic = null
        init(title, owner)
        this.contentText = content
        showAndWait().let { if (it.isPresent) it.get() else null }
    }
}

private val fileChooser = FileChooser().apply {
    History.latest?.let(::File)?.let {
        if (it.isDirectory) {
            initialDirectory = it
        } else if (it.exists()) {
            initialDirectory = it.parentFile
        }
    }
}

private val directoryChooser = DirectoryChooser()

private fun getExtensionName(ext: String): String {
    return App.optTr("misc.ext.$ext") ?: App.tr("misc.ext.common", ext.toUpperCase())
}

private fun setExtensionFilters(chooser: FileChooser, extensions: Collection<Collection<String>>, selected: String) {
    val filters = chooser.extensionFilters.apply { clear() }
    for (extension in extensions) {
        FileChooser.ExtensionFilter(getExtensionName(extension.first()), extension.map { "*.$it" }).apply {
            filters += this
            if (selected in extension) {
                chooser.selectedExtensionFilter = this
            }
        }
    }
}

fun selectDirectory(title: String, owner: Window = Imabw.fxApp.stage): File? {
    directoryChooser.title = title
    return directoryChooser.showDialog(owner)
}

private fun parserExtensions(): List<Set<String>> {
    return EpmManager.services.filter { it.hasParser && "crawler" !in it.keys }.map { it.keys }
}

private fun makerExtensions(): List<Set<String>> {
    return EpmManager.services.filter { it.hasMaker }.map { it.keys }
}

private fun makeSaveResult(file: File, chooser: FileChooser): Pair<File, String> {
    var extension = file.extension
    val extensions = chooser.selectedExtensionFilter.extensions
    if (extensions.find { extension in it } == null) {
        extension = extensions.first().substring(2) // trim '*.'
    }
    return file to extension
}

fun openBookFile(owner: Window = Imabw.fxApp.stage): File? {
    fileChooser.title = App.tr("d.openBook.title")
    setExtensionFilters(fileChooser, parserExtensions(), PMAB_NAME)
    return fileChooser.showOpenDialog(owner)?.also { fileChooser.initialDirectory = it.parentFile }
}

fun saveBookFile(name: String, format: String, owner: Window = Imabw.fxApp.stage): Pair<File, String>? {
    fileChooser.initialFileName = name
    fileChooser.title = App.tr("d.saveBook.title")
    fileChooser.extensionFilters.setAll(FileChooser.ExtensionFilter(getExtensionName(format), "*.$format"))
    return fileChooser.showSaveDialog(owner)?.let {
        fileChooser.initialDirectory = it.parentFile
        makeSaveResult(it, fileChooser)
    }
}

fun saveBookFile(name: String, owner: Window = Imabw.fxApp.stage): Pair<File, String>? {
    fileChooser.initialFileName = name
    fileChooser.title = App.tr("d.saveBook.title")
    setExtensionFilters(fileChooser, makerExtensions(), PMAB_NAME)
    return fileChooser.showSaveDialog(owner)?.let {
        fileChooser.initialDirectory = it.parentFile
        makeSaveResult(it, fileChooser)
    }
}

fun openBookFiles(owner: Window = Imabw.fxApp.stage): List<File>? {
    fileChooser.title = App.tr("d.openBook.title")
    setExtensionFilters(fileChooser, parserExtensions(), PMAB_NAME)
    return fileChooser.showOpenMultipleDialog(owner)?.also { fileChooser.initialDirectory = it.first().parentFile }
}

fun editAttributes(chapter: Chapter, owner: Window = Imabw.fxApp.stage) {
    println("edit attributes of $chapter")
}

fun editVariants(map: VariantMap, title: String, owner: Window = Imabw.fxApp.stage) {
    println("edit variants for $title: $map")
}
