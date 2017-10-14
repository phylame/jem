package jem.imabw.editor

import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.Tooltip
import jclp.isAncestor
import jclp.toRoot
import jem.Chapter
import jem.imabw.EditorSettings
import jem.imabw.Imabw
import jem.imabw.LoadTextTask
import jem.imabw.Workbench
import jem.imabw.ui.Editable
import jem.title
import mala.App
import mala.ixin.CommandHandler
import mala.ixin.IxIn
import mala.ixin.init
import org.fxmisc.richtext.ClipboardActions
import org.fxmisc.richtext.LineNumberFactory
import org.fxmisc.richtext.StyleClassedTextArea
import org.fxmisc.richtext.StyledTextArea
import org.fxmisc.undo.UndoManagerFactory

object EditorPane : TabPane(), CommandHandler {
    private val tabMenu = ContextMenu()
    private val textMenu = ContextMenu()

    val selectedTab get() = selectionModel.selectedItem as? ChapterTab

    init {
        id = "editor-pane"
        Imabw.register(this)

        val appDesigner = Imabw.dashboard.appDesigner
        appDesigner.items["tabContext"]?.let {
            tabMenu.init(it, Imabw, App, App.assets, IxIn.actionMap, null)
        }
        appDesigner.items["textContext"]?.let {
            textMenu.init(it, Imabw, App, App.assets, IxIn.actionMap, null)
        }
        selectionModel.selectedItemProperty().addListener { _, old, new ->
            (old as? ChapterTab)?.let {
                it.contextMenu = null
                it.textArea.contextMenu = null
            }
            (new as? ChapterTab)?.let {
                it.contextMenu = tabMenu
                it.textArea.contextMenu = textMenu
                Platform.runLater { it.content.requestFocus() }
            }
        }
        Workbench.workProperty.addListener { _, work, _ ->
            work?.book?.let { book ->
                // todo don't cache tabs
                tabs.removeIf { (it as? ChapterTab)?.chapter?.let { book === it || book.isAncestor(it) } == true }
            }
        }
        initActions()
    }

    fun openText(chapter: Chapter, icon: Node? = null) {
        val tab = tabs.find { (it as? ChapterTab)?.chapter === chapter } ?: ChapterTab(chapter).also { tabs += it }
        tab.graphic = icon
        selectionModel.select(tab)
    }

    fun closeText(chapter: Chapter) {
        tabs.removeIf { (it as? ChapterTab)?.let { it.chapter === chapter || chapter.isAncestor(it.chapter) } == true }
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
            "closeUnmodifiedTabs" -> tabs.removeIf { (it as? ChapterTab)?.isModified != true }
            "lock" -> selectedTab?.textArea?.let { it.isEditable = !it.isEditable }
            else -> return false
        }
        return true
    }
}

class ChapterTab(val chapter: Chapter) : Tab(chapter.title) {
    val textArea = TextEditor()
    var initialUndoPosition = textArea.undoManager.currentPosition

    var isModified
        get() = initialUndoPosition !== textArea.undoManager.currentPosition
        set(value) {
            println(initialUndoPosition)
            if (!value) {
                initialUndoPosition = textArea.undoManager.currentPosition
                println(initialUndoPosition)
            }
        }

    init {
        content = textArea

        val paths = chapter.toRoot().let { it.subList(1, it.size) }
        if (paths.isNotEmpty()) {
            tooltip = Tooltip(paths.joinToString(" > ") { it.title })
        }

        textArea.isWrapText = EditorSettings.wrapText
        textArea.setUndoManager(UndoManagerFactory.unlimitedHistoryFactory())
        textArea.paragraphGraphicFactory = LineNumberFactory.get(textArea) { "%${it}d" }
        chapter.text?.let {
            with(LoadTextTask(it)) {
                setOnSucceeded {
                    textArea.replaceText(value)
                    textArea.undoManager.forgetHistory()
                    textArea.moveTo(0)
                    hideProgress()
                }
                Imabw.submit(this)
            }
        }
    }
}

class TextEditor : StyleClassedTextArea(), Editable {
    override fun onEdit(command: String) {
        when (command) {
            "undo" -> undo()
            "redo" -> redo()
            "cut" -> cutParagraph()
            "copy" -> copyParagraph()
            "paste" -> paste()
            "delete" -> replaceSelection("")
            "selectAll" -> selectAll()
        }
    }
}

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
