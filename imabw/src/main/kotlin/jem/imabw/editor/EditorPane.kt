package jem.imabw.editor

import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.scene.control.ContextMenu
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import jem.Chapter
import jem.imabw.Imabw
import jem.imabw.ui.EditableComponent
import jem.title
import mala.App
import mala.ixin.CommandHandler
import mala.ixin.init
import org.fxmisc.richtext.LineNumberFactory
import org.fxmisc.richtext.StyleClassedTextArea
import org.fxmisc.richtext.StyledTextArea
import org.fxmisc.undo.UndoManagerFactory

object EditorPane : TabPane(), CommandHandler, EditableComponent {
    private val tabMenu = ContextMenu()
    private val textMenu = ContextMenu()

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
                Platform.runLater {
                    it.textArea.requestFocus()
                    initTextActions(it.textArea)
                }
            }
        }
        initActions()
    }

    val isActive get() = selectionModel.selectedItem?.content?.isFocused == true

    fun openText(chapter: Chapter) {
        val tab = tabs.find { (it as? ChapterTab)?.chapter === chapter } ?: ChapterTab(chapter).also {
            tabs += it
        }
        selectionModel.select(tab)
    }

    private fun initActions() {
//        val isTextFocused = Bindings.createBooleanBinding(Callable {
//            itemProperty.value?.content?.let {
//                it.isFocused && (it is TextArea || it is HTMLEditor || it is StyledTextArea<*>)
//            } ?: false
//        }, itemProperty)
//        val notTextFocused = focusedProperty().not().or(isTextFocused.not())
//        Imabw.getAction("replace")?.disableProperty?.bind(notTextFocused)
//        Imabw.getAction("joinLine")?.disableProperty?.bind(notTextFocused)
//        Imabw.getAction("duplicateText")?.disableProperty?.bind(notTextFocused)
//        Imabw.getAction("toggleCase")?.disableProperty?.bind(notTextFocused)

        val tabCount = Bindings.size(tabs)
        val notMultiTabs = tabCount.lessThan(2)
        Imabw.getAction("nextTab")?.disableProperty?.bind(notMultiTabs)
        Imabw.getAction("previousTab")?.disableProperty?.bind(notMultiTabs)
        Imabw.getAction("closeOtherTabs")?.disableProperty?.bind(notMultiTabs)

        val noTabs = tabCount.isEqualTo(0)
        Imabw.getAction("closeTab")?.disableProperty?.bind(noTabs)
        Imabw.getAction("closeAllTabs")?.disableProperty?.bind(noTabs)
        Imabw.getAction("closeUnmodifiedTabs")?.disableProperty?.bind(noTabs)
    }

    private fun initTextActions(textArea: StyledTextArea<*>) {
        Imabw.getAction("undo")?.disableProperty?.bind(Bindings.not(textArea.undoAvailableProperty()))
        Imabw.getAction("redo")?.disableProperty?.bind(Bindings.not(textArea.redoAvailableProperty()))
    }

    override fun handle(command: String, source: Any): Boolean {
        when (command) {
            "nextTab" -> selectionModel.selectNext()
            "previousTab" -> selectionModel.selectPrevious()
            else -> return false
        }
        return true
    }

    override fun editCommand(command: String) {
        when (command) {
            "undo" -> selectionModel.selectedItem.myArea?.undo()
            "redo" -> selectionModel.selectedItem.myArea?.redo()
            "cut" -> selectionModel.selectedItem.myArea?.cut()
            "copy" -> selectionModel.selectedItem.myArea?.copy()
            "paste" -> selectionModel.selectedItem.myArea?.paste()
            "delete" -> selectionModel.selectedItem.myArea?.replaceSelection("")
            "selectAll" -> selectionModel.selectedItem.myArea?.selectAll()
        }
    }
}

class ChapterTab(val chapter: Chapter) : Tab(chapter.title) {
    val textArea = StyleClassedTextArea()
    var undoPosition = textArea.undoManager.currentPosition

    init {
        content = textArea
        textArea.isWrapText = true
        textArea.setUndoManager(UndoManagerFactory.unlimitedHistoryFactory())
        textArea.paragraphGraphicFactory = LineNumberFactory.get(textArea) { "%${it}d" }
        textArea.replaceText(chapter.text?.toString() ?: "")
        textArea.moveTo(0)
    }
}

private val Tab.myArea get() = (this as? ChapterTab)?.content as? StyledTextArea<*>
