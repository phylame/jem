package jem.imabw.editor

import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.TextArea
import jem.Chapter
import jem.title
import org.fxmisc.richtext.StyledTextArea

object EditorPane : TabPane() {
    val itemProperty = selectionModel.selectedItemProperty()

    init {
    }

    fun openText(chapter: Chapter) {
        val tab = tabs.find { (it as? ChapterTab)?.chapter === chapter } ?: ChapterTab(chapter).also {
            tabs += it
        }
        selectionModel.select(tab)
    }
}

class ChapterTab(val chapter: Chapter) : Tab(chapter.title) {
    val text = StyledTextArea()

    init {
        content = text.also {
            it.isWrapText = true
            it.text = chapter.text?.toString()
            it.promptText = "Type to input"
        }
    }
}
