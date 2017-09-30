package jem.imabw.editor

import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import jem.Chapter
import jem.title
import org.fxmisc.richtext.LineNumberFactory
import org.fxmisc.richtext.StyleClassedTextArea
import org.fxmisc.undo.UndoManager

object EditorPane : TabPane() {
    val itemProperty = selectionModel.selectedItemProperty()

    init {
        isFocusTraversable = false
    }

    fun openText(chapter: Chapter) {
        val tab = tabs.find { (it as? ChapterTab)?.chapter === chapter } ?: ChapterTab(chapter).also {
            tabs += it
        }
        selectionModel.select(tab)
    }
}

class ChapterTab(val chapter: Chapter) : Tab(chapter.title) {
    val textArea = StyleClassedTextArea()
    var undoPosition: UndoManager.UndoPosition? = null

    init {
//        textProperty().bind(Bindings.format(""))
        println(textArea.undoManager.currentPosition)

        content = textArea
        textArea.isWrapText = true
        textArea.paragraphGraphicFactory = LineNumberFactory.get(textArea)
        textArea.replaceText(chapter.text?.toString() ?: "")
        textArea.moveTo(0)
    }
}
