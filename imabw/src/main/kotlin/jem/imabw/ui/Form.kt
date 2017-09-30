package jem.imabw.ui

import javafx.application.Application
import javafx.beans.binding.Bindings
import javafx.beans.value.ChangeListener
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Control
import javafx.scene.control.Label
import javafx.scene.control.Separator
import javafx.scene.control.SplitPane
import javafx.scene.input.KeyCombination
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Background
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.stage.Stage
import jclp.text.TEXT_PLAIN
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
            val actionMap = Imabw.actionMap
            App.assets.propertiesFor("ui/keys.properties")?.forEach { k, v ->
                actionMap[k.toString()]?.accelerator = KeyCombination.valueOf(v.toString())
            }
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

        initRuler()

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

    private fun initRuler() {
        EditorPane.itemProperty.addListener { _, old, new ->
            (old as? ChapterTab)?.textArea?.also { text ->
                @Suppress("UNCHECKED_CAST")
                (text.properties[this] as? ChangeListener<Int>)?.let {
                    text.caretPositionProperty().removeListener(it)
                }
            }
            if (new is ChapterTab) {
                new.textArea.also { text ->
                    ChangeListener<Int> { _, _, _ ->
                        updateCaret(text.currentParagraph + 1, text.caretColumn + 1, text.selection.length)
                    }.let {
                        text.properties[this] = it
                        text.caretPositionProperty().addListener(it)
                    }
                    words.textProperty().bind(Bindings.convert(text.lengthProperty()))
                    updateCaret(text.currentParagraph + 1, text.caretColumn + 1, text.selection.length)
                }
                updateMime(new.chapter.text?.type?.toUpperCase() ?: TEXT_PLAIN.toUpperCase())
            } else {
                reset()
                words.textProperty().unbind()
            }
        }
    }
}
