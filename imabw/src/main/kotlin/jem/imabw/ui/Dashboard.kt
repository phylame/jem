package jem.imabw.ui

import javafx.beans.InvalidationListener
import javafx.beans.binding.Bindings
import javafx.beans.binding.StringBinding
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.SplitPane
import javafx.scene.control.Tooltip
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import jclp.log.Log
import jclp.text.TEXT_PLAIN
import jem.author
import jem.imabw.Imabw
import jem.imabw.UISettings
import jem.imabw.Work
import jem.imabw.Workbench
import jem.imabw.editor.ChapterTab
import jem.imabw.editor.EditorPane
import jem.imabw.editor.editor
import jem.imabw.toc.NavPane
import jem.title
import mala.App
import mala.ixin.*

class Dashboard : IApplication(), CommandHandler {
    private val TAG = "Dashboard"

    lateinit var splitPane: SplitPane

    lateinit var appDesigner: AppDesigner

    override fun init() {
        Imabw.register(this)
    }

    override fun setup(scene: Scene, appPane: AppPane) {
        appDesigner = App.assets.designerFor("ui/designer.json")!!
        scene.stylesheets += App.assets.resourceFor("ui/default.css")!!.toExternalForm()

        appPane.setup(appDesigner, actionMap, menuMap)
        actionMap.updateAccelerators(App.assets.propertiesFor("ui/keys.properties")!!)
        appPane.statusBar?.right = Indicator

        splitPane = SplitPane().also {
            it.id = "main-split-pane"
            it.items.addAll(NavPane, EditorPane)
            it.setDividerPosition(0, 0.24)
            SplitPane.setResizableWithParent(NavPane, false)
            appPane.center = it
        }

        Workbench.workProperty.addListener { _, old, new ->
            stage.titleProperty().bind(object : StringBinding() {
                init {
                    super.bind(new.pathProperty, new.modifiedProperty, new.titleProperty, new.authorProperty)
                }

                override fun dispose() {
                    super.unbind(old.pathProperty, old.modifiedProperty, new.titleProperty, new.authorProperty)
                }

                override fun computeValue() = buildTitle(Workbench.work)
            })
        }

        initActions()
        restoreState(UISettings)
        statusText = App.tr("status.ready")
    }

    override fun restoreState(settings: IxInSettings) {
        super.restoreState(settings)
        NavPane.isVisible = UISettings.navigationBarVisible
        if (!NavPane.isVisible) splitPane.items.remove(0, 1)
    }

    override fun saveState(settings: IxInSettings) {
        super.saveState(settings)
        UISettings.navigationBarVisible = NavPane.isVisible
    }

    internal fun dispose() {
        saveState(UISettings)
        stage.close()
    }

    private fun initActions() {
        actionMap["showToolbar"]?.selectedProperty?.bindBidirectional(appPane.toolBar!!.visibleProperty())
        actionMap["showStatusBar"]?.selectedProperty?.bindBidirectional(appPane.statusBar!!.visibleProperty())
        actionMap["showNavigateBar"]?.selectedProperty?.bindBidirectional(NavPane.visibleProperty())
        actionMap["toggleFullScreen"]?.let { action ->
            stage.fullScreenProperty().addListener { _, _, value -> action.isSelected = value }
            action.selectedProperty.addListener { _, _, value -> stage.isFullScreen = value }
        }
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
            "showToolbar", "showStatusBar", "toggleFullScreen" -> Unit // bound with property
            "showNavigateBar" -> {
                if (!NavPane.isVisible) {
                    splitPane.items.remove(0, 1)
                } else {
                    splitPane.items.add(0, NavPane)
                    splitPane.setDividerPosition(0, 0.24)
                }
            }
            in editActions -> stage.scene.focusOwner.let {
                if (it is Editable) {
                    it.onEdit(command)
                } else {
                    Log.d(TAG) { "focused object is not editable: $it" }
                }
            }
            else -> return false
        }
        return true
    }
}

object Indicator : HBox() {
    private val caret = Label().apply { tooltip = Tooltip(App.tr("status.caret.toast")) }

    private val words = Label().apply { tooltip = Tooltip(App.tr("status.words.toast")) }

    private val mime = Label().apply { tooltip = Tooltip(App.tr("status.mime.toast")) }

    init {
        id = "indicator"
        alignment = Pos.CENTER
        BorderPane.setAlignment(this, Pos.CENTER)

        caret.addEventHandler(MouseEvent.MOUSE_PRESSED) {
            if (it.clickCount == 1 && it.isPrimaryButtonDown) {
                Imabw.handle("goto", it.source)
            }
        }

        children.addAll(caret, words, mime)
        IxIn.newAction("lock").toButton(Imabw, Style.TOGGLE, hideText = true).also { children += it }
        IxIn.newAction("gc").toButton(Imabw, hideText = true).also { children += it }

        reset()
        initRuler()
    }

    fun reset() {
        updateCaret(-1, -1, 0)
        updateWords(-1)
        updateMime("")
    }

    fun updateCaret(row: Int, column: Int, selection: Int) {
        if (row < 0) {
            caret.isVisible = false
        } else {
            caret.isVisible = true
            caret.text = when {
                selection > 0 -> "$row:$column/$selection"
                else -> "$row:$column"
            }
        }
    }

    fun updateWords(count: Int) {
        if (count < 0) {
            words.isVisible = false
        } else {
            println(count)
            words.isVisible = true
            words.text = count.toString()
        }
    }

    fun updateMime(type: String) {
        if (type.isEmpty()) {
            mime.isVisible = false
        } else {
            mime.isVisible = true
            mime.text = type
        }
    }

    private fun initRuler() {
        EditorPane.selectionModel.selectedItemProperty().addListener { _, old, new ->
            old?.editor?.let { text ->
                @Suppress("UNCHECKED_CAST")
                (text.properties[this] as? InvalidationListener)?.let { listener ->
                    text.caretPositionProperty().removeListener(listener)
                    text.selectionProperty().removeListener(listener)
                }
            }
            if (new is ChapterTab) {
                new.textArea.also { text ->
                    InvalidationListener {
                        updateCaret(text.currentParagraph + 1, text.caretColumn + 1, text.selection.length)
                    }.let { listener ->
                        text.properties[this] = listener
                        text.caretPositionProperty().addListener(listener)
                        text.selectionProperty().addListener(listener)
                    }
                    words.isVisible = true
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
