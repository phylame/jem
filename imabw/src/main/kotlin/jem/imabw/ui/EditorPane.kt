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
import jclp.EventBus
import jclp.io.flobOf
import jclp.io.writeLines
import jclp.isAncestor
import jclp.log.Log
import jclp.text.TEXT_PLAIN
import jclp.text.textOf
import jclp.toRoot
import jem.Chapter
import jem.imabw.*
import jem.title
import mala.App
import mala.ixin.CommandHandler
import mala.ixin.IxIn
import mala.ixin.resetDisable
import mala.ixin.toContextMenu
import java.io.File

object EditorPane : TabPane(), CommandHandler, Editable {
    private val tabMenu: ContextMenu?
    private val textMenu: ContextMenu?

    val selectedTab get() = selectionModel.selectedItem as? ChapterTab

    val chapterTabs get() = tabs.asSequence().filterIsInstance<ChapterTab>()

    init {
        Imabw.register(this)
        App.registerCleanup { chapterTabs.forEach { it.dispose() } }

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
            new?.content?.let {
                when (it) {
                    is TextArea -> {
                        Indicator.words.isVisible = true
                        Indicator.words.textProperty().bind(it.lengthProperty().asString())
                    }
                }
            }

            (old as? ChapterTab)?.editor?.contextMenu = null
            (new as? ChapterTab)?.let {
                it.editor.contextMenu = textMenu
                Indicator.updateMime(it.chapter.text?.type?.toUpperCase() ?: TEXT_PLAIN.toUpperCase())
                Platform.runLater { it.content.requestFocus() }
            }

        }
        tabs.addListener(ListChangeListener {
            while (it.next()) {
                it.removed.forEach { (it as? ChapterTab)?.cacheIfNeed(true) }
            }
        })
        EventBus.register<WorkflowEvent> {
            if (it.what == WorkflowType.BOOK_SAVED) {
                chapterTabs.forEach { it.resetModification() }
            } else if (it.what == WorkflowType.BOOK_CLOSED) {
                removeTab(it.book)
            }
        }
        initActions()
    }

    fun openTab(chapter: Chapter, icon: Node?) {
        require(Workbench.work!!.book.isAncestor(chapter)) { "not child of current book" }
        val tab = tabs.find { (it as? ChapterTab)?.chapter === chapter } ?: ChapterTab(chapter).also { tabs += it }
        selectionModel.select(tab)
        tab.graphic = icon
    }

    fun removeTab(chapter: Chapter) {
        tabs.iterator().apply {
            while (hasNext()) {
                (next() as? ChapterTab)?.let {
                    if (chapter === it.chapter || chapter.isAncestor(it.chapter)) {
                        it.dispose()
                        remove()
                    }
                }
            }
        }
    }

    fun cacheTabs() {
        chapterTabs.forEach { it.cacheIfNeed(false) }
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
            "closeTab" -> tabs.remove(selectedTab)
            "closeAllTabs" -> tabs.clear()
            "closeOtherTabs" -> tabs.removeIf { it !== selectedTab }
            "closeUnmodifiedTabs" -> tabs.removeIf { (it as? ChapterTab)?.isModified != true }
            "lock" -> selectedTab?.editor?.let { it.isEditable = !it.isEditable }
            else -> return false
        }
        return true
    }

    override fun onEdit(command: String) {
        selectedTab?.editor?.onEdit(command)
    }
}

class ChapterTab(val chapter: Chapter) : Tab(chapter.title) {
    private val tagId = javaClass.simpleName

    private var cacheFile: File? = null

    @Volatile
    private var isReady = false

    var isModified = false
        private set

    val editor = TextEditor()

    var isDisposed = false
        private set

    init {
        content = editor
        chapter.toRoot().let { it.subList(1, it.size) }.takeIf { it.isNotEmpty() }?.let {
            tooltip = Tooltip(it.joinToString(" > ") { it.title })
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
        } else with(LoadTextTask(text)) {
            setOnRunning {
                updateProgress(App.tr("jem.loadText.hint", chapter.title))
            }
            setOnSucceeded {
                editor.text = value
                editor.positionCaret(0)
                isReady = true
                hideProgress()
            }
            Imabw.submit(this)
        }
    }

    fun cacheText() {
        Log.d(tagId) { "cache text for '${chapter.title}'" }
        if (editor.length <= 512) {
            chapter.text = textOf(editor.text)
            return
        }
        File.createTempFile("imabw-text-", ".txt").let {
            try {
                it.bufferedWriter().use { it.writeLines(editor.paragraphs.iterator()) }
                chapter.text = textOf(flobOf(it, "text/plain"), Charsets.UTF_8)
            } catch (e: Exception) {
                Log.e(tagId, e) { "cannot cache text to '$it'" }
                if (it.delete()) {
                    Log.e(tagId) { "cannot delete cache file: $it" }
                }
            }
            cacheFile = it
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

    fun resetModification() {
        isModified = false
    }

    fun dispose() {
        isDisposed = true
        if (cacheFile?.delete() == false) {
            Log.e(tagId) { "cannot delete cache file: $cacheFile" }
        }
    }
}

class TextEditor : TextArea(), Editable {
    init {
        isWrapText = EditorSettings.wrapText
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
    }

    private fun initActions() {
        val actionMap = IxIn.actionMap
        val notEditable = editableProperty().not()
        actionMap["cut"]?.disableProperty?.bind(notEditable)
        actionMap["copy"]?.resetDisable()
        actionMap["paste"]?.disableProperty?.bind(notEditable)
        actionMap["delete"]?.disableProperty?.bind(notEditable)
        actionMap["undo"]?.disableProperty?.bind(undoableProperty().not())
        actionMap["redo"]?.disableProperty?.bind(redoableProperty().not())
        actionMap["find"]?.resetDisable()
        actionMap["findNext"]?.resetDisable()
        actionMap["findPrevious"]?.resetDisable()
        actionMap["replace"]?.disableProperty?.bind(notEditable)
        actionMap["lock"]?.let {
            it.isSelected = !isEditable
            it.isDisable = false
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
        }
    }
}
