package jem.imabw.ui

import javafx.application.Application
import javafx.beans.binding.Bindings
import javafx.beans.value.ChangeListener
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.Separator
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.stage.Modality
import javafx.stage.Stage
import jclp.text.TEXT_PLAIN
import jclp.toRoot
import jem.Chapter
import jem.author
import jem.imabw.History
import jem.imabw.Imabw
import jem.imabw.Workbench
import jem.imabw.editor.ChapterTab
import jem.imabw.editor.EditorPane
import jem.imabw.toc.NavPane
import jem.title
import mala.App
import mala.ixin.*

class Form : Application(), CommandHandler {
    lateinit var stage: Stage
        private set

    lateinit var appPane: AppPane
        private set

    lateinit var appDesigner: AppDesigner

    var statusText
        get() = appPane.statusBar?.statusLabel?.text
        set(value) {
            appPane.statusBar?.statusLabel?.text = value
        }

    override fun init() {
        Imabw.form = this
        Imabw.register(this)
    }

    override fun start(stage: Stage) {
        this.stage = stage
        appDesigner = App.assets.designerFor("ui/main.idj")!!

        appPane = AppPane().also { pane ->
            pane.setup(appDesigner)
            pane.statusBar?.right = Indicator
            Imabw.actionMap.updateAccelerators(App.assets.propertiesFor("ui/keys.properties")!!)
        }
        SplitPane().also { pane ->
            pane.items += NavPane
            pane.items += EditorPane.also {
                stage.titleProperty().bind(CommonBinding(it.selectionModel.selectedItemProperty()) {
                    buildTitle((it.value as? ChapterTab)?.chapter)
                })
            }
            pane.setDividerPosition(0, 0.24)
            appPane.center = pane
        }

        initActions()

        stage.icons += App.assets.imageFor("icon")
        stage.setOnCloseRequest {
            it.consume()
            Imabw.handle("exit", stage)
        }
        stage.scene = Scene(appPane, 1080.0, 607.0).also {
            it.stylesheets += App.assets.resourceFor("ui/default.css")?.toExternalForm()
        }
        stage.show()

        Workbench.init()
    }

    fun dispose() {
        stage.close()
    }

    private fun initActions() {
        Imabw.getAction("clearHistory")?.disableProperty?.bind(Bindings.isEmpty(History.items))
        Imabw.getAction("showToolbar")?.selectedProperty?.bindBidirectional(appPane.toolBar!!.visibleProperty())
        Imabw.getAction("showStatusBar")?.selectedProperty?.bindBidirectional(appPane.statusBar!!.visibleProperty())
    }

    private fun buildTitle(chapter: Chapter?) = StringBuilder().run {
        chapter?.toRoot()?.let {
            val book = it.first()
            append(book.title).append(" - ")
            book.author.takeIf { it.isNotEmpty() }?.let {
                append("[").append(it).append("] - ")
            }
            append(it.subList(1, it.size).joinToString("\\") { it.title }).append(" - ")
        }
        append("PW Imabw ")
        append(Imabw.version)
        toString()
    }

    override fun handle(command: String, source: Any): Boolean {
        when (command) {
            "showToolbar", "showStatusBar" -> Unit // bound with property
            in editActions -> when {
                NavPane.isActive -> NavPane.onEdit(command)
                EditorPane.isActive -> EditorPane.onEdit(command)
            }
            else -> return false
        }
        return true
    }
}

inline fun textInput(title: String, tip: String, text: String, block: (String) -> Unit) {
    with(TextInputDialog(text)) {
        graphic = null
        headerText = null
        this.title = title
        this.contentText = tip
        initOwner(Imabw.form.stage)
        initModality(Modality.WINDOW_MODAL)
        showAndWait().let { if (it.isPresent) block(it.get()) }
    }
}

object Indicator : HBox() {
    private val caret = Label()

    private val words = Label()

    private val mime = Label()

    init {
        id = "indicator"
        alignment = Pos.CENTER
        BorderPane.setAlignment(this, Pos.CENTER)

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
        this += node.also { it.isFocusTraversable = false }
    }

    private fun initRuler() {
        EditorPane.selectionModel.selectedItemProperty().addListener { _, old, new ->
            (old as? ChapterTab)?.textArea?.let { text ->
                @Suppress("UNCHECKED_CAST")
                (text.properties[this] as? ChangeListener<Int>)?.let { listener ->
                    text.caretPositionProperty().removeListener(listener)
                }
                words.textProperty().unbind()
            }
            if (new is ChapterTab) {
                new.textArea.also { text ->
                    ChangeListener<Int> { _, _, _ ->
                        updateCaret(text.currentParagraph + 1, text.caretColumn + 1, text.selection.length)
                    }.let { listener ->
                        text.properties[this] = listener
                        text.caretPositionProperty().addListener(listener)
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

private val editActions = arrayOf(
        "undo", "redo", "cut", "copy", "paste", "delete", "selectAll", "find", "findNext", "findPrevious"
)

interface Editable {
    fun onEdit(command: String)
}
