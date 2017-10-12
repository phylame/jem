package jem.imabw.editor

import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import jclp.isAncestor
import jclp.text.Text
import jem.Chapter
import jem.imabw.Imabw
import jem.imabw.LoadTextTask
import jem.imabw.execute
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
                Platform.runLater { it.textArea.requestFocus() }
            }
        }
        initActions()
    }

    val isActive get() = selectionModel.selectedItem?.content?.isFocused == true

    fun openText(chapter: Chapter, icon: Node? = null) {
        val tab = tabs.find { it.chapter === chapter } ?: ChapterTab(chapter).also { tabs += it }
        tab.graphic = icon
        selectionModel.select(tab)
    }

    fun closeText(chapter: Chapter) {
        tabs.removeIf { it.chapter === chapter || chapter.isAncestor(it.chapter!!) }
    }

    private fun initActions() {
        val actionMap = Imabw.actionMap

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
            scene.focusOwnerProperty().addListener { _, _, new ->
                val actions = arrayOf("replace", "joinLine", "duplicateText", "toggleCase")
                if (new is StyledTextArea<*>) {
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
            "closeUnmodifiedTabs" -> tabs.removeIf { it.isModified != true }
            "lock" -> selectedTab?.textArea?.let { it.isEditable = !it.isEditable }
            else -> return false
        }
        return true
    }

    override fun onEdit(command: String) {
        val editor = selectedTab?.textArea ?: return
        when (command) {
            "undo" -> editor.undo()
            "redo" -> editor.redo()
            "cut" -> editor.cutParagraph()
            "copy" -> editor.copyParagraph()
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
        chapter.text?.let { loadText(it) }
    }

    private fun loadText(text: Text) {
        val task = LoadTextTask(text)
        task.setOnSucceeded {
            textArea.replaceText(task.value)
            textArea.undoManager.forgetHistory()
            textArea.moveTo(0)
        }
        task.execute()
    }
}

internal val Tab.chapter get() = (this as? ChapterTab)?.chapter

internal val Tab.isModified get() = (this as? ChapterTab)?.isModified

internal val Tab.editor get() = (this as? ChapterTab)?.content as? StyledTextArea<*>

fun ClipboardActions<*>.cutParagraph() {
    if (selection.length > 0) {
        cut()
    } else {
        println("cut paragraph")
    }
}

fun ClipboardActions<*>.copyParagraph() {
    if (selection.length > 0) {
        copy()
    } else {
        println("copy paragraph")
    }
}
