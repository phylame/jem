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

import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Window
import jclp.ValueMap
import jclp.text.ifNotEmpty
import jclp.traceText
import jem.Chapter
import jem.epm.EpmManager
import jem.epm.FileParser
import jem.epm.PMAB_NAME
import jem.imabw.*
import jem.title
import mala.App
import mala.ixin.graphicFor
import java.io.File
import java.util.concurrent.Callable

fun Dialog<*>.init(title: String, owner: Window, tag: String = "") {
    headerText = null
    this.title = title
    initModality(Modality.WINDOW_MODAL)
    initOwner(owner)
    if (tag.isNotEmpty()) {
        setOnShowing { UISettings.restoreState(this, tag) }
        setOnHidden { UISettings.storeState(this, tag) }
    }
}

fun alert(type: Alert.AlertType, title: String, content: String, owner: Window = Imabw.topWindow): Alert {
    return Alert(type, content).apply { init(title, owner) }
}

fun info(title: String, content: String, owner: Window = Imabw.topWindow) {
    alert(Alert.AlertType.INFORMATION, title, content, owner).showAndWait()
}

fun error(title: String, content: String, owner: Window = Imabw.topWindow) {
    alert(Alert.AlertType.ERROR, title, content, owner).showAndWait()
}

fun confirm(title: String, content: String, owner: Window = Imabw.topWindow): Boolean {
    return with(alert(Alert.AlertType.CONFIRMATION, title, content, owner)) {
        showAndWait().get() == ButtonType.OK
    }
}

fun debug(title: String, content: String, throwable: Throwable, owner: Window) {
    with(alert(Alert.AlertType.NONE, title, content, owner)) {
        val textArea = TextArea().apply {
            isEditable = false
            isFocusTraversable = false
            styleClass += "error-text"
        }
        dialogPane.expandableContent = textArea
        dialogPane.buttonTypes += ButtonType.CLOSE
        graphic = App.assets.graphicFor("dialog/bug")
        dialogPane.expandedProperty().addListener { _ ->
            if (textArea.text.isEmpty()) textArea.text = throwable.traceText
        }
        showAndWait()
    }
}

fun input(
        title: String,
        label: String,
        initial: String,
        canEmpty: Boolean = true,
        mustDiff: Boolean = false,
        owner: Window = Imabw.topWindow
): String? {
    return with(TextInputDialog(initial)) {
        graphic = null
        init(title, owner)
        val textField = editor
        textField.prefColumnCount = 20
        val okButton = dialogPane.lookupButton(ButtonType.OK)
        if (!canEmpty) {
            if (mustDiff) {
                okButton.disableProperty().bind(Bindings.createBooleanBinding(Callable {
                    textField.text.let { it.isEmpty() || it == initial }
                }, textField.textProperty()))
            } else {
                okButton.disableProperty().bind(Bindings.isEmpty(textField.textProperty()))
            }
        } else if (mustDiff) {
            okButton.disableProperty().bind(Bindings.equal(textField.textProperty(), initial))
        }
        contentText = label
        showAndWait().let { if (it.isPresent) it.get() else null }
    }
}

fun text(
        title: String,
        initial: String,
        canEmpty: Boolean = true,
        mustDiff: Boolean = false,
        owner: Window = Imabw.topWindow
): String? {
    with(Dialog<ButtonType>()) {
        init(title, owner, "text")
        val root = VBox().apply {
            dialogPane.content = this
        }
        val textArea = TextArea().apply {
            text = initial
            isWrapText = true
            root.children += this
            selectAll()
        }
        val openButton = ButtonType(App.tr("ui.button.open"), ButtonBar.ButtonData.LEFT)
        dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL, openButton)
        dialogPane.lookupButton(openButton).isDisable = true
        val okButton = dialogPane.lookupButton(ButtonType.OK)
        if (!canEmpty) {
            if (mustDiff) {
                okButton.disableProperty().bind(Bindings.createBooleanBinding(Callable {
                    textArea.text.let { it.isEmpty() || it == initial }
                }, textArea.textProperty()))
            } else {
                okButton.disableProperty().bind(Bindings.isEmpty(textArea.textProperty()))
            }
        } else if (mustDiff) {
            okButton.disableProperty().bind(Bindings.equal(textArea.textProperty(), initial))
        }
        Platform.runLater { textArea.requestFocus() }
        return if (showAndWait().get() == ButtonType.OK) textArea.text else null
    }
}

private val fileChooser = FileChooser().apply {
    (Workbench.work?.path ?: History.latest)?.let { File(it) }?.let {
        if (it.isDirectory) {
            initialDirectory = it
        } else if (it.exists()) {
            initialDirectory = it.parentFile
        }
    }
}

private val directoryChooser = DirectoryChooser()

private val allExtensionFilter = FileChooser.ExtensionFilter(getExtensionName("any"), "*.*")

private fun getExtensionName(ext: String): String {
    return App.optTr("misc.ext.$ext") ?: App.tr("misc.ext.common", ext.toUpperCase())
}

private fun setExtensionFilters(chooser: FileChooser, extensions: Collection<Collection<String>>, selected: String) {
    val filters = chooser.extensionFilters
    for (extension in extensions) {
        FileChooser.ExtensionFilter(getExtensionName(extension.first()), extension.map { "*.$it" }).apply {
            filters += this
            if (selected in extension) {
                chooser.selectedExtensionFilter = this
            }
        }
    }
}

fun selectOpenFile(title: String, owner: Window): File? {
    fileChooser.title = title
    fileChooser.extensionFilters.clear()
    return fileChooser.showOpenDialog(owner)
}

fun selectSaveFile(title: String, initName: String = "", owner: Window): File? {
    fileChooser.title = title
    fileChooser.extensionFilters.setAll(allExtensionFilter)
    fileChooser.initialFileName = initName
    return fileChooser.showSaveDialog(owner)
}

fun selectDirectory(title: String, owner: Window): File? {
    directoryChooser.title = title
    return directoryChooser.showDialog(owner)
}

fun selectOpenImage(title: String, owner: Window): File? {
    fileChooser.title = title
    GeneralSettings.lastImageDir.ifNotEmpty { fileChooser.initialDirectory = File(it) }
    fileChooser.extensionFilters.setAll(allExtensionFilter)
    setExtensionFilters(fileChooser, setOf(
            setOf("jpg", "jpeg"),
            setOf("png"),
            setOf("bmp"),
            setOf("gif")
    ), "")
    return fileChooser.showOpenDialog(owner)?.apply { GeneralSettings.lastImageDir = parent }
}

private fun parserExtensions(): List<Set<String>> {
    return EpmManager.services.filter { it.hasParser && it is FileParser }.map { it.keys }
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

fun openBookFile(owner: Window): File? {
    fileChooser.title = App.tr("d.openBook.title")
    fileChooser.extensionFilters.clear()
    setExtensionFilters(fileChooser, parserExtensions(), PMAB_NAME)
    GeneralSettings.lastBookDir.ifNotEmpty { fileChooser.initialDirectory = File(it) }
    return fileChooser.showOpenDialog(owner)?.also { GeneralSettings.lastBookDir = it.parent }
}

fun saveBookFile(name: String, format: String, owner: Window): Pair<File, String>? {
    fileChooser.initialFileName = name
    fileChooser.title = App.tr("d.saveBook.title")
    fileChooser.extensionFilters.setAll(FileChooser.ExtensionFilter(getExtensionName(format), "*.$format"))
    GeneralSettings.lastBookDir.ifNotEmpty { fileChooser.initialDirectory = File(it) }
    return fileChooser.showSaveDialog(owner)?.let {
        GeneralSettings.lastBookDir = it.parent
        makeSaveResult(it, fileChooser)
    }
}

fun saveBookFile(name: String, owner: Window): Pair<File, String>? {
    fileChooser.initialFileName = name
    fileChooser.title = App.tr("d.saveBook.title")
    fileChooser.extensionFilters.clear()
    setExtensionFilters(fileChooser, makerExtensions(), PMAB_NAME)
    GeneralSettings.lastBookDir.ifNotEmpty { fileChooser.initialDirectory = File(it) }
    return fileChooser.showSaveDialog(owner)?.let {
        GeneralSettings.lastBookDir = it.parent
        makeSaveResult(it, fileChooser)
    }
}

fun openBookFiles(owner: Window): List<File>? {
    fileChooser.title = App.tr("d.openBook.title")
    fileChooser.extensionFilters.clear()
    setExtensionFilters(fileChooser, parserExtensions(), PMAB_NAME)
    GeneralSettings.lastBookDir.ifNotEmpty { fileChooser.initialDirectory = File(it) }
    return fileChooser.showOpenMultipleDialog(owner)?.also { GeneralSettings.lastBookDir = it.first().parent }
}

fun editAttributes(chapter: Chapter, owner: Window): Boolean {
    with(Dialog<ButtonType>()) {
        isResizable = true
        init(App.tr("d.editAttribute.title", chapter.title), owner, "attributes")
        val pane = AttributePane(chapter).apply { dialogPane.content = BorderPane(this) }
        dialogPane.buttonTypes.setAll(ButtonType.OK, ButtonType.CANCEL)
        dialogPane.content.style += "-fx-padding: 0"
        val result = showAndWait().get()
        pane.storeState()
        return if (result == ButtonType.OK && pane.isModified) {
            pane.syncVariants()
            true
        } else false
    }
}

fun editVariants(map: ValueMap, title: String, ignoredKeys: Set<String> = emptySet(), owner: Window): Boolean {
    with(Dialog<ButtonType>()) {
        isResizable = true
        init(title, owner, "variants")
        val pane = object : VariantPane(map, "variants", false) {
            override fun ignoredKeys() = ignoredKeys
        }
        dialogPane.content = pane
        dialogPane.content.style += "-fx-padding: 0"
        dialogPane.buttonTypes.setAll(ButtonType.OK, ButtonType.CANCEL)
        val result = showAndWait().get()
        pane.storeState()
        return if (result == ButtonType.OK && pane.isModified) {
            pane.syncVariants()
            true
        } else false
    }
}
