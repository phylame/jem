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

package jem.imabw.editor

import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.value.ChangeListener
import javafx.collections.ListChangeListener
import javafx.scene.Node
import javafx.scene.control.*
import jclp.EventBus
import jclp.flob.flobOf
import jclp.io.writeLines
import jclp.isAncestor
import jclp.isSelfOrAncestor
import jclp.log.Log
import jclp.text.textOf
import jclp.toRoot
import jem.Chapter
import jem.imabw.*
import jem.imabw.ui.Editable
import jem.title
import mala.App
import mala.ixin.CommandHandler
import mala.ixin.IxIn
import mala.ixin.init
import org.fxmisc.richtext.LineNumberFactory
import org.fxmisc.richtext.StyleClassedTextArea
import org.fxmisc.richtext.model.NavigationActions
import java.io.File


object EditorPane : TabPane(), CommandHandler {
    private val tabMenu = ContextMenu()
    private val textMenu = ContextMenu()

    val selectedTab get() = selectionModel.selectedItem as? ChapterTab

    val chapterTabs get() = tabs.asSequence().filterIsInstance<ChapterTab>()

    init {
        id = "editor-pane"
        Imabw.register(this)
        App.registerCleanup { dispose() }

        val designer = Imabw.dashboard.appDesigner
        designer.items["tabContext"]?.let {
            tabMenu.init(it, Imabw, App, App.assets, IxIn.actionMap, null)
        }
        designer.items["textContext"]?.let {
            textMenu.init(it, Imabw, App, App.assets, IxIn.actionMap, null)
        }
        selectionModel.selectedItemProperty().addListener { _, old, new ->
            (old as? ChapterTab)?.let {
                it.contextMenu = null
                it.textEditor.contextMenu = null
            }
            (new as? ChapterTab)?.let {
                it.contextMenu = tabMenu
                it.textEditor.contextMenu = textMenu
                Platform.runLater { it.content.requestFocus() }
            }
        }
        tabs.addListener(ListChangeListener {
            while (it.next()) {
                it.removed.forEach { (it as? ChapterTab)?.cacheIfNeed() }
            }
        })
        EventBus.register<WorkflowEvent> {
            if (it.what == WorkflowType.BOOK_SAVED) {
                chapterTabs.forEach { it.resetModified() }
            } else if (it.what == WorkflowType.BOOK_CLOSED) {
                removeTab(it.source)
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
                    if (chapter.isSelfOrAncestor(it.chapter)) {
                        it.dispose()
                        remove()
                    }
                }
            }
        }
    }

    fun cacheTexts() {
        chapterTabs.filter { it.isModified }.forEach { it.cache() }
    }

    internal fun dispose() {
        chapterTabs.forEach { it.dispose() }
    }

    private fun initActions() {
        val actionMap = IxIn.actionMap

        val tabCount = Bindings.size(tabs)
        val notMultiTabs = tabCount.lessThan(2)
        actionMap["nextTab"]?.disableProperty?.bind(notMultiTabs)
        actionMap["previousTab"]?.disableProperty?.bind(notMultiTabs)
        actionMap["closeOtherTabs"]?.disableProperty?.bind(notMultiTabs)

        val noTabs = tabCount.isEqualTo(0)
        actionMap["closeTab"]?.disableProperty?.bind(noTabs)
        actionMap["closeAllTabs"]?.disableProperty?.bind(noTabs)
        actionMap["closeUnmodifiedTabs"]?.disableProperty?.bind(noTabs)

        sceneProperty().addListener { _, _, scene ->
            scene?.focusOwnerProperty()?.addListener { _, _, new ->
                val actions = arrayOf("replace")
                if (new is TextEditor) {
                    val notEditable = new.editableProperty().not()
                    for (action in actions) {
                        actionMap[action]?.disableProperty?.bind(notEditable)
                    }
                    actionMap["cut"]?.disableProperty?.bind(notEditable)
                    actionMap["copy"]?.let {
                        it.disableProperty.unbind()
                        it.isDisable = false
                    }
                    actionMap["paste"]?.disableProperty?.bind(notEditable)
                    actionMap["delete"]?.disableProperty?.bind(notEditable)
                    actionMap["undo"]?.disableProperty?.bind(notEditable.or(Bindings.not(new.undoAvailableProperty())))
                    actionMap["redo"]?.disableProperty?.bind(notEditable.or(Bindings.not(new.redoAvailableProperty())))
                    actionMap["lock"]?.let {
                        it.isSelected = !new.isEditable
                        it.isDisable = false
                    }
                } else {
                    actions.mapNotNull { actionMap[it] }.forEach {
                        it.disableProperty.unbind()
                        it.isDisable = true
                    }
                    actionMap["lock"]?.let {
                        it.isSelected = false
                        it.isDisable = true
                    }
                }
            }
        }
    }

    override fun handle(command: String, source: Any): Boolean {
        when (command) {
            "nextTab" -> selectionModel.selectNext()
            "previousTab" -> selectionModel.selectPrevious()
            "closeTab" -> tabs.remove(selectedTab)
            "closeAllTabs" -> tabs.clear()
            "closeOtherTabs" -> tabs.removeIf { it !== selectedTab }
            "closeUnmodifiedTabs" -> tabs.removeIf { (it as? ChapterTab)?.isModified != true }
            "lock" -> selectedTab?.textEditor?.let { it.isEditable = !it.isEditable }
            else -> return false
        }
        return true
    }
}

class ChapterTab(val chapter: Chapter) : Tab(chapter.title) {
    private val tagId = javaClass.simpleName

    val isModified get() = !textEditor.undoManager.isAtMarkedPosition

    val textEditor = TextEditor()

    private var isReady = false

    var isDisposed = false
        private set

    init {
        content = textEditor

        val paths = chapter.toRoot().let { it.subList(1, it.size) }
        if (paths.isNotEmpty()) {
            tooltip = Tooltip(paths.joinToString(" > ") { it.title })
        }

//        textEditor.plainTextChanges().addObserver {
//            if (isReady && isModified) {
//                EventBus.post(ModificationEvent(chapter, ModificationType.TEXT_MODIFIED))
//            }
//        }
        textEditor.undoManager.atMarkedPositionProperty().addListener { observable, oldValue, newValue ->
            println("$oldValue,$newValue")
        }

        textEditor.isWrapText = EditorSettings.wrapText
        textEditor.paragraphGraphicFactory = LineNumberFactory.get(textEditor) { "%${it}d" }
        val text = chapter.text
        if (text == null) {
            textEditor.undoManager.mark()
            isReady = true
        } else {
            with(LoadTextTask(text)) {
                setOnRunning {
                    updateProgress(App.tr("jem.loadText.hint", chapter.title))
                }
                setOnSucceeded {
                    textEditor.replaceText(value)
                    textEditor.undoManager.forgetHistory()
                    textEditor.undoManager.mark()
                    hideProgress()
                    isReady = true
                }
                Imabw.submit(this)
            }
        }
    }

    internal fun resetModified() {
        textEditor.undoManager.mark()
    }

    private var cacheFile: File? = null

    fun cache() {
        Log.t(tagId) { "cache text for '${chapter.title}'" }
        if (textEditor.document.length() <= 1024) {
            chapter.text = textOf(textEditor.text)
            return
        }
        File.createTempFile("imabw-text-", ".txt").let {
            try {
                it.bufferedWriter().use { it.writeLines(textEditor.paragraphs) }
                chapter.text = textOf(flobOf(it.toPath(), "text/plain"), "UTF-8")
            } catch (e: Exception) {
                Log.e(tagId, e) { "cannot cache text to '$it'" }
                if (it.delete()) {
                    Log.e(tagId) { "cannot delete cache file: $it" }
                }
            }
            cacheFile = it
        }
    }

    fun cacheIfNeed() {
        if (isModified && !isDisposed) {
            Imabw.submit { cache() }
        }
    }

    fun dispose() {
        isDisposed = true
        if (cacheFile?.delete() == false) {
            Log.e(tagId) { "cannot delete cache file: $cacheFile" }
        }
    }
}

class TextEditor : StyleClassedTextArea(false), Editable {
    override fun cut() {
        selectLineIfEmpty()
        super.cut()
    }

    override fun copy() {
        val oldSelection = selectLineIfEmpty()
        super.copy()
        if (oldSelection != null)
            selectRange(oldSelection.start, oldSelection.end)
    }

    private fun selectLineIfEmpty(): IndexRange? {
        var oldSelection: IndexRange? = null
        if (selectedText.isEmpty()) {
            oldSelection = selection
            selectLine()
            nextChar(NavigationActions.SelectionPolicy.ADJUST)
        }
        return oldSelection
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
