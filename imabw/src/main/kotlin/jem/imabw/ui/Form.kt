package jem.imabw.ui

import javafx.application.Application
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.Separator
import javafx.scene.control.SplitPane
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.stage.Stage
import jem.imabw.Imabw
import jem.imabw.Workbench
import jem.imabw.editor.EditorPane
import jem.imabw.toc.NavPane
import mala.App
import mala.ixin.AppPane
import mala.ixin.designerFor
import mala.ixin.plusAssign

class Form : Application() {
    lateinit var appPane: AppPane

    lateinit var splitPane: SplitPane

    lateinit var statusBar: BorderPane
    lateinit var statusText: Label
    lateinit var indicator: Indicator

    override fun init() {
        Imabw.form = this
    }

    override fun start(stage: Stage) {
        appPane = AppPane()
        appPane.setup(App.assets.designerFor("ui/main.idj")!!)

        splitPane = SplitPane().also {
            it.items += NavPane
            it.items += EditorPane
            it.setDividerPosition(0, 0.24)
        }
        appPane.center = splitPane

        statusBar = BorderPane().also { pane ->
            pane.styleClass += "status-bar"
            pane.padding = Insets(2.0, 4.0, 2.0, 4.0)

            statusText = Label().apply {
                BorderPane.setAlignment(this, Pos.CENTER)
                pane.left = this
            }

            indicator = Indicator().apply {
                BorderPane.setAlignment(this, Pos.CENTER)
                pane.right = this
            }

            appPane.statusBar = pane
        }

        statusText.text = "Ready"
        stage.title = "Untitled - PW Imabw ${Imabw.version}"
        stage.setOnCloseRequest { Imabw.handle("exit", stage) }
        stage.scene = Scene(appPane)
        stage.show()

        Workbench.init()
    }
}

class Indicator : HBox() {
    private val caret = Label()

    private val words = Label()

    private val mimeType = Label()

    private val encoding = Label()

    init {
        alignment = Pos.CENTER

        caret.addEventHandler(MouseEvent.MOUSE_PRESSED) {
            if (it.clickCount == 1 && it.isPrimaryButtonDown) {
                Imabw.handle("goto", it.source)
            }
        }
        add(caret)
        add(words)
        add(mimeType)
        add(encoding)

        updateCaret(-1, -1, 0)
        updateWords(-1)
        updateMimeType("")
        updateEncoding("")
    }

    fun updateCaret(row: Int, column: Int, selection: Int) {
        caret.text = when {
            row < 0 -> "n/a"
            selection > 0 -> "$row:$column/$selection"
            else -> "$row:$column"
        }
    }

    fun updateWords(count: Int) {
        words.text = if (count < 0) "n/a" else count.toString()
    }

    fun updateMimeType(type: String) {
        mimeType.text = if (type.isEmpty()) "n/a" else type
    }

    fun updateEncoding(name: String) {
        encoding.text = if (name.isEmpty()) "n/a" else name
    }

    private fun add(node: Node) {
        this += Separator(Orientation.VERTICAL)
        this += node
    }
}
