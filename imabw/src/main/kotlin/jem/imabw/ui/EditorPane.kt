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
import javafx.collections.ListChangeListener
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.input.KeyCombination
import javafx.scene.layout.GridPane
import jclp.EventBus
import jclp.ifNotEmpty
import jclp.io.flobOf
import jclp.io.writeLines
import jclp.isSelfOrAncestor
import jclp.log.Log
import jclp.text.TEXT_PLAIN
import jclp.text.or
import jclp.text.textOf
import jclp.toRoot
import jem.Chapter
import jem.imabw.*
import jem.title
import mala.App
import mala.App.tr
import mala.ixin.*
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

object EditorPane : TabPane(), CommandHandler, EditAware {
    private const val TAG = "EditorPane"

    private val tabMenu: ContextMenu?
    private val textMenu: ContextMenu?

    val currentTab inline get() = selectionModel.selectedItem as? ChapterTab

    init {
        Imabw.register(this)
        App.registerCleanup {
            for (tab in tabs) {
                (tab as? ChapterTab)?.dispose()
            }
        }

        id = "editor-pane"
        isFocusTraversable = false
        tabClosingPolicy = TabClosingPolicy.ALL_TABS

        val designer = Imabw.dashboard.designer
        tabMenu = designer.items["tabContext"]?.toContextMenu(Imabw, App, App.assets, IxIn.actionMap, null)
        textMenu = designer.items["textContext"]?.toContextMenu(Imabw, App, App.assets, IxIn.actionMap, null)
        selectionModel.selectedItemProperty().addListener { _, old, new ->
            Indicator.words.textProperty().unbind()
            Indicator.reset()

            old?.contextMenu = null
            new?.contextMenu = tabMenu

            new?.content?.let { node ->
                when (node) {
                    is TextArea -> {
                        Indicator.words.isVisible = true
                        Indicator.words.textProperty().bind(node.lengthProperty().asString())
                    }
                }
            }

            (old as? ChapterTab)?.editor?.contextMenu = null
            (new as? ChapterTab)?.let { tab ->
                tab.editor.contextMenu = textMenu
                Indicator.updateMime(tab.chapter.text?.type?.toUpperCase() ?: TEXT_PLAIN.toUpperCase())
                Platform.runLater { tab.content.requestFocus() }
            }

        }
        tabs.addListener(ListChangeListener { change ->
            while (change.next()) {
                change.removed.forEach { (it as? ChapterTab)?.cacheIfNeed(true) }
            }
        })
        EventBus.register<WorkflowEvent> { event ->
            if (event.what == WorkflowType.BOOK_SAVED) {
                for (tab in tabs) {
                    (tab as? ChapterTab)?.resetModifiedState()
                }
            } else if (event.what == WorkflowType.BOOK_CLOSED) {
                removeTab(event.book)
            }
        }
        initActions()
    }

    fun openTab(chapter: Chapter, icon: Node?) {
//        require(Workbench.work!!.book.isAncestor(chapter)) { "not child of current book" }
        val tab = tabs.find { (it as? ChapterTab)?.chapter === chapter } ?: ChapterTab(chapter).also { tabs += it }
        selectionModel.select(tab)
        tab.graphic = icon
    }

    fun removeTab(chapter: Chapter) {
        tabs.iterator().apply {
            while (hasNext()) {
                (next() as? ChapterTab)?.let { tab ->
                    if (chapter.isSelfOrAncestor(tab.chapter)) {
                        tab.dispose()
                        remove()
                    }
                }
            }
        }
    }

    fun cacheTabs(chapter: Chapter? = null) {
        Log.t(TAG) { "cache text in tabs" }
        for (tab in tabs) {
            (tab as? ChapterTab)?.let {
                if (chapter?.isSelfOrAncestor(it.chapter) != false) {
                    it.cacheIfNeed(false)
                }
            }
        }
    }

    private fun initActions() {
        val actionMap = IxIn.actionMap

        val notMultiTabs = Bindings.size(tabs).lessThan(2)
        actionMap["nextTab"]?.disableProperty?.bind(notMultiTabs)
        actionMap["previousTab"]?.disableProperty?.bind(notMultiTabs)
        actionMap["closeOtherTabs"]?.disableProperty?.bind(notMultiTabs)

        val noTabs = Bindings.isEmpty(tabs)
        actionMap["closeTab"]?.disableProperty?.bind(noTabs)
        actionMap["closeAllTabs"]?.disableProperty?.bind(noTabs)
        actionMap["closeUnmodifiedTabs"]?.disableProperty?.bind(noTabs)
    }

    override fun handle(command: String, source: Any): Boolean {
        when (command) {
            "nextTab" -> selectionModel.selectNext()
            "previousTab" -> selectionModel.selectPrevious()
            "closeTab" -> tabs.remove(currentTab)
            "closeAllTabs" -> tabs.clear()
            "closeOtherTabs" -> tabs.removeIf { it !== currentTab }
            "closeUnmodifiedTabs" -> tabs.removeIf { (it as? ChapterTab)?.isModified != true }
            "lock" -> currentTab?.editor?.let { it.isEditable = !it.isEditable }
            else -> return false
        }
        return true
    }

    override fun onEdit(command: String) {
        currentTab?.editor?.onEdit(command)
    }
}

class ChapterTab(val chapter: Chapter) : Tab(chapter.title) {
    private val tagId = javaClass.simpleName

    private var cacheFile: Path? = null

    @Volatile
    private var isReady = false

    val editor = TextEditor()

    var isModified = false
        private set

    var isDisposed = false
        private set

    init {
        content = editor
        chapter.toRoot().let { it.subList(1, it.size) }.ifNotEmpty { paths ->
            tooltip = Tooltip(paths.joinToString(" > ") { it.title })
        }
        editor.textProperty().addListener { _ ->
            if (isReady) {
                isModified = true
                EventBus.post(ModificationEvent(chapter, ModificationType.TEXT_MODIFIED, 1))
            }
        }
        loadText()
    }

    private fun loadText() {
        val text = chapter.text
        if (text == null) {
            isReady = true
        } else {
            with(LoadTextTask(text, tr("jem.loadText.hint", chapter.title))) {
                setOnSucceeded {
                    editor.text = value
                    editor.positionCaret(0)
                    isReady = true
                    hideProgress()
                }
                Imabw.submit(this)
            }
        }
    }

    fun cacheText() {
        if (editor.length <= 512) {
            chapter.text = textOf(editor.text)
            return
        }
        Files.createTempFile("text-", ".txt").let { path ->
            try {
                Log.d(tagId) { "cache text for '${chapter.title}' to '$path'" }
                Files.newBufferedWriter(path).use { it.writeLines(editor.paragraphs.iterator()) }
                chapter.text = textOf(flobOf(path, "text/plain"), Charsets.UTF_8)
                tempFiles.add(path)
                cacheFile = path
            } catch (e: IOException) {
                Log.e(tagId, e) { "cannot cache text of '${chapter.title}' to '$path'" }
                try {
                    Files.deleteIfExists(path)
                } catch (e: IOException) {
                    Log.e(tagId, e) { "cannot delete temp file: '$path'" }
                }
            }
        }
    }

    fun cacheIfNeed(async: Boolean) {
        if (isModified && !isDisposed) {
            if (async) {
                Imabw.submit { cacheText() }
            } else {
                cacheText()
            }
        }
    }

    fun resetModifiedState() {
        isModified = false
    }

    fun dispose() {
        isDisposed = true
        cacheFile?.let { path ->
            try {
                Files.deleteIfExists(path)
            } catch (e: IOException) {
                Log.e(tagId, e) { "cannot delete cache file: '$path'" }
            }
        }
    }
}

class TextEditor : TextArea(), EditAware {
    // find/replace fields
    private var lastPosition: Int = 0
    private var lastText: String = ""
    private var lastReplace: String = ""

    init {
        isWrapText = EditorSettings.wrapText
        promptText = tr("main.editor.prompt")
        focusedProperty().addListener { _, _, focused ->
            if (focused) {
                initActions()
            } else {
                IxIn.actionMap["lock"]?.let {
                    it.isSelected = false
                    it.isDisable = true
                }
            }
        }
        bindKeyActions()
    }

    private fun initActions() {
        val actionMap = IxIn.actionMap
        val notEditable = editableProperty().not()
        val isEmpty = lengthProperty().isEqualTo(0)
        actionMap["cut"]?.disableProperty?.bind(notEditable)
        actionMap["copy"]?.resetDisable()
        actionMap["paste"]?.disableProperty?.bind(notEditable)
        actionMap["delete"]?.disableProperty?.bind(notEditable)
        actionMap["undo"]?.disableProperty?.bind(undoableProperty().not())
        actionMap["redo"]?.disableProperty?.bind(redoableProperty().not())
        actionMap["find"]?.disableProperty?.bind(isEmpty)
        actionMap["findNext"]?.disableProperty?.bind(isEmpty)
        actionMap["findPrevious"]?.disableProperty?.bind(isEmpty)
        actionMap["replace"]?.disableProperty?.bind(notEditable.or(isEmpty))
        actionMap["lock"]?.let {
            it.isSelected = !isEditable
            it.isDisable = false
        }
    }

    // in text area, accelerator without no modifier does't work
    private fun bindKeyActions() {
        val map = hashMapOf<String, KeyCombination>()
        for (entry in Imabw.dashboard.accelerators) {
            entry.value.asKeyCombination?.let {
                if (it.shortcut == KeyCombination.ModifierValue.UP
                        && it.shift == KeyCombination.ModifierValue.UP
                        && it.alt == KeyCombination.ModifierValue.UP) {
                    map[entry.key.toString()] = it
                }
            }
        }
        map.remove("delete")
        setOnKeyPressed { event ->
            for (entry in map) {
                if (entry.value.match(event)) {
                    event.consume()
                    Imabw.handle(entry.key, this)
                    break
                }
            }
        }
    }

    override fun onEdit(command: String) {
        when (command) {
            "undo" -> undo()
            "redo" -> redo()
            "cut" -> cut()
            "copy" -> copy()
            "paste" -> paste()
            "delete" -> replaceSelection("")
            "selectAll" -> selectAll()
            "find" -> {
                if (length != 0) {
                    input(tr("d.findText.title"), tr("d.findText.hint"), selectedText or lastText, canEmpty = false)?.let { str ->
                        findAndGo(str)
                    }
                }
            }
            "findNext" -> {
                if (length != 0 && !lastText.isEmpty()) findAndGo(lastText)
            }
            "findPrevious" -> {
                if (length != 0 && !lastText.isEmpty()) findPrevious()
            }
            "replace" -> replaceText()
            else -> println("ignore edit action: $command")
        }
    }

    private fun findAndGo(str: String) {
        val position = text.indexOf(str, minOf(caretPosition, lastPosition))
        if (position != -1) {
            selectRange(position, position + str.length)
            lastPosition = position + str.length
            lastText = str
        }
    }

    private fun findPrevious() {
        val str = lastText
        val position = text.lastIndexOf(str, minOf(caretPosition, lastPosition))
        if (position != -1) {
            selectRange(position, position + str.length)
            lastPosition = position - str.length
        }
    }

    private fun replaceText() {
        if (length == 0) return
        with(alert(Alert.AlertType.NONE, tr("d.replaceText.title"), "")) {
            buttonTypes.setAll(ButtonType.OK, ButtonType.CANCEL)
            val fromField = TextField(selectedText or lastText).apply {
                selectAll()
                prefColumnCount = 20
                dialogPane.lookupButton(ButtonType.OK).disableProperty().bind(lengthProperty().isEqualTo(0))
            }
            val toField = TextField(lastReplace).apply {
                selectAll()
                prefColumnCount = 20
            }
            dialogPane.content = GridPane().apply {
                initAsForm(listOf(
                        Label(tr("d.replaceText.source")), Label(tr("d.replaceText.target"))
                ), listOf(
                        fromField, toField
                ))
            }
            fromField.requestFocus()
            if (showAndWait().get() == ButtonType.OK) {

            }
        }
    }
}
