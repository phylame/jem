package jem.imabw.editor

import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.scene.control.ContextMenu
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import jclp.isAncestor
import jem.Chapter
import jem.imabw.Imabw
import jem.imabw.ui.Editable
import jem.title
import mala.App
import mala.ixin.CommandHandler
import mala.ixin.init
import org.fxmisc.richtext.ClipboardActions
import org.fxmisc.richtext.LineNumberFactory
import org.fxmisc.richtext.StyleClassedTextArea
import org.fxmisc.richtext.StyledTextArea
import org.fxmisc.undo.UndoManagerFactory
import java.util.concurrent.Callable

object EditorPane : TabPane(), CommandHandler, Editable {
    private val tabMenu = ContextMenu()
    private val textMenu = ContextMenu()

    val selectedTab get() = selectionModel.selectedItem as? ChapterTab

    init {
        id = "editor-pane"
        Imabw.register(this)

        Imabw.form.appDesigner.items["tabContext"]?.let {
            tabMenu.init(it, Imabw, App, App.assets, Imabw.actionMap, null)
        }
        Imabw.form.appDesigner.items["textContext"]?.let {
            textMenu.init(it, Imabw, App, App.assets, Imabw.actionMap, null)
        }
        selectionModel.selectedItemProperty().addListener { _, old, new ->
            (old as? ChapterTab)?.let {
                it.contextMenu = null
                it.textArea.contextMenu = null
            }
            (new as? ChapterTab)?.let {
                it.contextMenu = tabMenu
                it.textArea.contextMenu = textMenu
                initEditActions(it.textArea)
                Platform.runLater {
                    it.textArea.requestFocus()
                }
            }
        }
        initActions()
    }

    val isActive get() = selectionModel.selectedItem?.content?.isFocused == true

    fun openText(chapter: Chapter) {
        val tab = tabs.find { it.chapter === chapter } ?: ChapterTab(chapter).also { tabs += it }
        selectionModel.select(tab)
    }

    fun closeText(chapter: Chapter) {
        tabs.removeIf { it.chapter === chapter || chapter.isAncestor(it.chapter!!) }
    }

    private fun initActions() {
        val actionMap = Imabw.actionMap

        sceneProperty().addListener { _, _, scene ->
            val notTextFocused = Bindings.createBooleanBinding(Callable {
                scene.focusOwner !is StyledTextArea<*>
            }, scene.focusOwnerProperty())
            actionMap["replace"]?.disableProperty?.bind(notTextFocused)
            actionMap["joinLine"]?.disableProperty?.bind(notTextFocused)
            actionMap["duplicateText"]?.disableProperty?.bind(notTextFocused)
            actionMap["toggleCase"]?.disableProperty?.bind(notTextFocused)
        }

        val tabCount = Bindings.size(tabs)
        val notMultiTabs = tabCount.lessThan(2)
        actionMap["nextTab"]?.disableProperty?.bind(notMultiTabs)
        actionMap["previousTab"]?.disableProperty?.bind(notMultiTabs)
        actionMap["closeOtherTabs"]?.disableProperty?.bind(notMultiTabs)

        val noTabs = tabCount.isEqualTo(0)
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
            "closeUnmodifiedTabs" -> tabs.removeIf { it.isModified != true }
            else -> return false
        }
        return true
    }

    private fun initEditActions(textArea: StyledTextArea<*>) {
        val actionMap = Imabw.actionMap

        actionMap["undo"]?.disableProperty?.bind(Bindings.not(textArea.undoAvailableProperty()))
        actionMap["redo"]?.disableProperty?.bind(Bindings.not(textArea.redoAvailableProperty()))
    }

    override fun onEdit(command: String) {
        val editor = selectedTab?.textArea ?: return
        when (command) {
            "undo" -> editor.undo()
            "redo" -> editor.redo()
            "cut" -> {
                if (editor.selection.length > 0) {
                    editor.cut()
                } else {
                    editor.cutParagraph()
                }
            }
            "copy" -> {
                if (editor.selection.length > 0) {
                    editor.copy()
                } else {
                    editor.copyParagraph()
                }
            }
            "paste" -> editor.paste()
            "delete" -> editor.replaceSelection("")
            "selectAll" -> editor.selectAll()
        }
    }
}

class ChapterTab(val chapter: Chapter) : Tab(chapter.title) {
    val textArea = StyleClassedTextArea()
    var undoPosition = textArea.undoManager.currentPosition
    var isModified = false

    init {
        content = textArea
        textArea.isWrapText = true
        textArea.setUndoManager(UndoManagerFactory.unlimitedHistoryFactory())
        textArea.paragraphGraphicFactory = LineNumberFactory.get(textArea) { "%${it}d" }
        textArea.replaceText(chapter.text?.toString() ?: "")
        textArea.moveTo(0)
    }
}

private val Tab.chapter get() = (this as? ChapterTab)?.chapter

private val Tab.isModified get() = (this as? ChapterTab)?.isModified

private val Tab.editor get() = (this as? ChapterTab)?.content as? StyledTextArea<*>

fun ClipboardActions<*>.cutParagraph() {

}

fun ClipboardActions<*>.copyParagraph() {

}
