package jem.imabw.ui

import javafx.application.Application
import javafx.beans.binding.Bindings
import javafx.beans.binding.StringBinding
import javafx.beans.value.ChangeListener
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.Separator
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.stage.Stage
import jclp.text.TEXT_PLAIN
import jem.author
import jem.imabw.Imabw
import jem.imabw.UISettings
import jem.imabw.Work
import jem.imabw.Workbench
import jem.imabw.editor.ChapterTab
import jem.imabw.editor.EditorPane
import jem.imabw.editor.editor
import jem.imabw.plugin.ImabwAddon
import jem.imabw.toc.NavPane
import jem.title
import mala.App
import mala.ixin.*

class Form : Application(), CommandHandler {
    lateinit var stage: Stage
        private set

    lateinit var appPane: AppPane
        private set

    lateinit var splitPane: SplitPane

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
        appDesigner = App.assets.designerFor("ui/main.json")!!

        appPane = AppPane().also {
            it.setup(appDesigner)
            it.statusBar?.right = Indicator
            Imabw.actionMap.updateAccelerators(App.assets.propertiesFor("ui/keys.properties")!!)
        }
        splitPane = SplitPane().also {
            it.items += NavPane
            it.items += EditorPane
            it.setDividerPosition(0, 0.24)
            SplitPane.setResizableWithParent(NavPane, false)
            appPane.center = it
        }

        initActions()
        Workbench.workProperty.addListener { _, old, new ->
            stage.titleProperty().bind(object : StringBinding() {
                init {
                    super.bind(new.pathProperty, new.modifiedProperty)
                }

                override fun dispose() {
                    super.unbind(old.pathProperty, old.modifiedProperty)
                }

                override fun computeValue() = buildTitle(Workbench.work)
            })
        }
        stage.icons += App.assets.imageFor("icon")
        stage.setOnCloseRequest {
            it.consume()
            Imabw.handle("exit", stage)
        }
        stage.scene = Scene(appPane).also {
            it.stylesheets += App.assets.resourceFor("ui/default.css")?.toExternalForm()
        }
        stage.x = UISettings.formX
        stage.y = UISettings.formY
        stage.width = UISettings.formWidth
        stage.height = UISettings.formHeight
        if (UISettings.formFullScreen) {
            stage.isFullScreen = true
        } else if (UISettings.formMaximized) {
            stage.isMaximized = true
        }
        stage.show()
        statusText = App.tr("status.ready")
        Workbench.ready()

        App.plugins.with<ImabwAddon> { ready() }
    }

    fun dispose() {
        UISettings.formX = stage.x
        UISettings.formY = stage.y
        UISettings.formWidth = stage.width
        UISettings.formHeight = stage.height
        UISettings.formFullScreen = stage.isFullScreen
        UISettings.formMaximized = stage.isMaximized
        stage.close()
    }

    private val progressLabel = Label()

    fun beginProgress() {
        appPane.statusBar?.left = HBox(4.0).apply {
            alignment = Pos.CENTER
            BorderPane.setAlignment(this, Pos.CENTER)
            children += ProgressIndicator().also { it.id = "status-progress" }
            children += progressLabel
        }
    }

    fun updateProgress(text: String) {
        progressLabel.text = text
    }

    fun endProgress() {
        appPane.statusBar?.left = appPane.statusBar?.statusLabel
    }

    private fun initActions() {
        val actionMap = Imabw.actionMap
        actionMap["showToolbar"]?.selectedProperty?.bindBidirectional(appPane.toolBar!!.visibleProperty())
        actionMap["showStatusBar"]?.selectedProperty?.bindBidirectional(appPane.statusBar!!.visibleProperty())
        actionMap["showNavigateBar"]?.selectedProperty?.bindBidirectional(NavPane.visibleProperty())
    }

    private fun buildTitle(work: Work) = StringBuilder().run {
        val book = work.book
        if (work.isModified) {
            append("*")
        }
        append(book.title).append(" - ")
        book.author.takeIf { it.isNotEmpty() }?.let {
            append("[").append(it).append("] - ")
        }
        work.path?.let {
            append(it).append(" - ")
        }
        append("PW Imabw ")
        append(Imabw.version)
        toString()
    }

    override fun handle(command: String, source: Any): Boolean {
        when (command) {
            "showToolbar", "showStatusBar" -> Unit // bound with property
            "showNavigateBar" -> {
                if (!NavPane.isVisible) {
                    splitPane.items.remove(0, 1)
                } else {
                    splitPane.items.add(0, NavPane)
                    splitPane.setDividerPosition(0, 0.24)
                }
            }
            in editActions -> when {
                NavPane.isActive -> NavPane.onEdit(command)
                EditorPane.isActive -> EditorPane.onEdit(command)
            }
            else -> return false
        }
        return true
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

        Imabw.newAction("lock").toButton(Imabw, Style.TOGGLE, hideText = true).also {
            add(it)
        }

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
            old?.editor?.let { text ->
                @Suppress("UNCHECKED_CAST")
                (text.properties[this] as? ChangeListener<Int>)?.let { listener ->
                    text.caretPositionProperty().removeListener(listener)
                }
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
                words.textProperty().unbind()
                reset()
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
