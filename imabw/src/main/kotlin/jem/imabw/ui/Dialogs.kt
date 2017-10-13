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
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Window
import jclp.VariantMap
import jem.Chapter
import jem.epm.EpmManager
import jem.imabw.History
import jem.imabw.Imabw
import mala.App
import java.io.File

fun inputText(title: String, tip: String, text: String) = with(TextInputDialog(text)) {
    graphic = null
    headerText = null
    this.title = title
    this.contentText = tip
    initOwner(Imabw.fxApp.stage)
    initModality(Modality.WINDOW_MODAL)
    showAndWait().let { if (it.isPresent) it.get() else null }
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

fun selectDirectory(owner: Window, title: String): File? {
    directoryChooser.title = title
    return directoryChooser.showDialog(owner)
}

private fun parserExtensions(): List<Set<String>> {
    return EpmManager.services.filter { it.hasParser && "crawler" !in it.keys }.map { it.keys }
}

private fun makerExtensions(): List<Set<String>> {
    return EpmManager.services.filter { it.hasMaker }.map { it.keys }
}

fun openBookFile(owner: Window): File? {
    fileChooser.title = App.tr("d.openBook.title")
    setExtensionFilters(fileChooser, parserExtensions(), "pmab")
    return fileChooser.showOpenDialog(owner)?.also { fileChooser.initialDirectory = it.parentFile }
}

fun saveBookFile(owner: Window, name: String = ""): File? {
    fileChooser.initialFileName = name
    fileChooser.title = App.tr("d.saveBook.title")
    setExtensionFilters(fileChooser, makerExtensions(), "pmab")
    return fileChooser.showSaveDialog(owner)?.also { fileChooser.initialDirectory = it.parentFile }
}

fun openBookFiles(owner: Window): List<File>? {
    fileChooser.title = App.tr("d.openBook.title")
    setExtensionFilters(fileChooser, parserExtensions(), "pmab")
    return fileChooser.showOpenMultipleDialog(owner)?.also { fileChooser.initialDirectory = it.first().parentFile }
}

fun editAttributes(chapter: Chapter) {
    println("edit attributes of $chapter")
}

fun editVariants(map: VariantMap, title: String) {
    println("edit variants for $title: $map")
}
