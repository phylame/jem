package jem.imabw.ui

import javafx.application.Application
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Control
import javafx.scene.control.Label
import javafx.scene.control.Separator
import javafx.scene.control.SplitPane
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Background
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.stage.Stage
import jclp.toRoot
import jem.Chapter
import jem.author
import jem.imabw.Imabw
import jem.imabw.Workbench
import jem.imabw.editor.ChapterTab
import jem.imabw.editor.EditorPane
import jem.imabw.toc.NavPane
import jem.title
import mala.App
import mala.ixin.*

class Form : Application() {
    lateinit var stage: Stage
        private set

    lateinit var appPane: AppPane
        private set

    lateinit var splitPane: SplitPane
        private set

    lateinit var statusBar: BorderPane
    lateinit var statusText: Label
    lateinit var indicator: Indicator

    override fun init() {
        Imabw.form = this
        Imabw.register(this)
    }

    override fun start(stage: Stage) {
        this.stage = stage

        appPane = AppPane().also { pane ->
            pane.setup(App.assets.designerFor("ui/main.idj")!!)
        }

        splitPane = SplitPane().also { pane ->
            pane.items += NavPane
            pane.items += EditorPane.also {
                it.itemProperty.addListener { _, _, tab ->
                    stage.title = buildTitle((tab as? ChapterTab)?.chapter)
                }
            }
            pane.setDividerPosition(0, 0.24)
            appPane.center = pane
        }

        statusBar = BorderPane().also { pane ->
            pane.styleClass += "status-bar"

            statusText = Label().also {
                BorderPane.setAlignment(it, Pos.CENTER)
                it.padding = Insets(0.0, 0.0, 0.0, 4.0)
                pane.left = it
            }

            indicator = Indicator().also {
                BorderPane.setAlignment(it, Pos.CENTER)
                pane.right = it
            }

            appPane.statusBar = pane
        }

        stage.title = buildTitle(null)
        stage.icons += App.assets.imageFor("icon")
        stage.setOnCloseRequest {
            it.consume()
            Imabw.handle("exit", stage)
        }
        stage.scene = Scene(appPane)
        stage.show()

        Workbench.init()
    }

    fun dispose() {
        stage.close()
    }

    private fun buildTitle(chapter: Chapter?) = StringBuilder().run {
        chapter?.toRoot()?.let {
            val book = it.first()
            append(book.title).append(" - ")
            book.author.takeIf { it.isNotEmpty() }?.let {
                append("[").append(it).append("] - ")
            }
            append(it.joinToString("\\") { it.title }).append(" - ")
        }
        append("PW Imabw ")
        append(Imabw.version)
        toString()
    }
}

class Indicator : HBox() {
    private val caret = Label()

    private val words = Label()

    private val mime = Label()

    init {
        alignment = Pos.CENTER
        padding = Insets(0.0, 2.0, 0.0, 0.0)
        styleClass += "indicator"

        caret.addEventHandler(MouseEvent.MOUSE_PRESSED) {
            if (it.clickCount == 1 && it.isPrimaryButtonDown) {
                Imabw.handle("goto", it.source)
            }
        }
        EditorPane.itemProperty.addListener { _, _, tab ->
            if (tab is ChapterTab) {

                updateWords(tab.text.length)
                updateMime(tab.chapter.text?.type?:"")
            } else {
                reset()
            }
        }

        add(caret)
        add(words)
        add(mime)

        Imabw.newAction("gc").toButton(Imabw, hideText = true).also {
            it.background = Background.EMPTY
            it.padding = Insets.EMPTY
            add(it)
        }

        reset()
    }

    fun reset() {
        updateCaret(-1, -1, 0)
        updateWords(-1)
        updateMime("")
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

    fun updateMime(type: String) {
        mime.text = if (type.isEmpty()) "n/a" else type
    }

    private fun add(node: Control) {
        this += Separator(Orientation.VERTICAL)
        this += node.also {
            it.padding = Insets(2.0, 2.0, 2.0, 2.0)
        }
    }
}
