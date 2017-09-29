package jem.imabw.editor

import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.TextArea
import javafx.scene.control.Tooltip

object EditorPane : TabPane() {
    init {
        for (i in 1..7) {
            tabs += Tab("Chapter $i", TextArea()).also {
                it.tooltip = Tooltip("This is chapter $i")
            }
        }
    }
}
