package jem.imabw.toc

import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ListChangeListener
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Background
import javafx.scene.layout.BorderPane
import javafx.util.Callback
import jem.Book
import jem.Chapter
import jem.imabw.Imabw
import jem.imabw.Work
import jem.imabw.Workbench
import jem.imabw.editor.EditorPane
import jem.intro
import jem.title
import mala.App
import mala.App.tr
import mala.ixin.*
import org.json.JSONArray
import org.json.JSONTokener

typealias ChapterNode = TreeItem<Chapter>

object NavPane : BorderPane(), CommandHandler {
    private val root = ChapterNode()
    private val tree = TreeView(root)

    val itemProperty = tree.selectionModel.selectedItemProperty()

    init {
        Imabw.register(this)

        top = NavHeader
        center = tree

        tree.isShowRoot = false
        tree.cellFactory = CellFactory
        tree.selectionModel.selectionMode = SelectionMode.MULTIPLE
        tree.addEventHandler(MouseEvent.MOUSE_PRESSED) {
            if (it.clickCount == 2 && it.isPrimaryButtonDown) {
                itemProperty.value?.value?.takeIf { !it.isSection && it !is Book }?.let(EditorPane::openText)
            }
        }

        initActions()

        Workbench.tasks.addListener(ListChangeListener<Work> {
            while (it.next()) {
                if (it.wasAdded()) {
                    it.addedSubList.asSequence().map { it.book.toTreeItem() }.toCollection(root.children)
                    tree.selectionModel.clearSelection()
                    tree.selectionModel.selectLast()
                } else if (it.wasRemoved()) {
                    val books = it.removed.map { it.book }
                    root.children.removeIf { it.value in books }
                }
            }
        })
    }

    private fun initActions() {
        val isTreeFocused = tree.focusedProperty()
        Imabw.getAction("replace")?.disableProperty?.bind(isTreeFocused)
        Imabw.getAction("joinLine")?.disableProperty?.bind(isTreeFocused)
        Imabw.getAction("duplicateText")?.disableProperty?.bind(isTreeFocused)
        Imabw.getAction("toggleCase")?.disableProperty?.bind(isTreeFocused)

        val isMultiSelection = SimpleBooleanProperty()
        tree.selectionModel.selectedItems.addListener(ListChangeListener {
            isMultiSelection.value = it.list.size > 1
        })
        val isBookSelected = SimpleBooleanProperty()
        tree.selectionModel.selectedItems.addListener(ListChangeListener {
            isBookSelected.value = it.list.find { it.parent === root } != null
        })

        Imabw.getAction("newChapter")?.disableProperty?.bind(isMultiSelection)
        Imabw.getAction("insertChapter")?.disableProperty?.bind(isMultiSelection.or(isBookSelected))
        Imabw.getAction("importChapter")?.disableProperty?.bind(isMultiSelection)
        Imabw.getAction("exportChapter")?.disableProperty?.bind(isBookSelected)
        Imabw.getAction("renameChapter")?.disableProperty?.bind(isMultiSelection)
        Imabw.getAction("mergeChapter")?.disableProperty?.bind(isMultiSelection.not().or(isBookSelected))
        Imabw.getAction("viewAttributes")?.disableProperty?.bind(isMultiSelection)

        val noBookSelected = isBookSelected.not()
        val notSingleBook = isMultiSelection.or(noBookSelected)
        Imabw.getAction("bookAttributes")?.disableProperty?.bind(notSingleBook)
        Imabw.getAction("bookExtensions")?.disableProperty?.bind(notSingleBook)

        Imabw.getAction("closeFile")?.disableProperty?.bind(noBookSelected)
        Imabw.getAction("saveFile")?.disableProperty?.bind(noBookSelected)
        Imabw.getAction("saveAsFile")?.disableProperty?.bind(noBookSelected)
        Imabw.getAction("duplicateFile")?.disableProperty?.bind(noBookSelected)
        Imabw.getAction("lockFile")?.disableProperty?.bind(noBookSelected)

        Imabw.getAction("fileDetails")?.disableProperty?.bind(notSingleBook)
    }

    @Command
    fun newChapter() {
        val parent = itemProperty.value
        val chapter = Chapter("Chapter ${parent.children.size + 1}")
        addChapters(parent, -1, listOf(chapter))
    }

    @Command
    fun insertChapter() {
        val parent = itemProperty.value.parent
        val chapter = Chapter("Chapter ${parent.children.size + 1}")
        addChapters(parent, parent.children.indexOf(itemProperty.value), listOf(chapter))
    }

    fun addChapters(parentNode: ChapterNode, index: Int, chapters: Collection<Chapter>) {
        val children = parentNode.children // children is lazied
        val parentChapter = parentNode.value
        val selectionModel = tree.selectionModel
        selectionModel.clearSelection()
        chapters.map { it.toTreeItem() }.forEachIndexed { i, item ->
            if (index < 0) {
                children += item
                parentChapter += item.value
            } else {
                children.add(index + i, item)
                parentChapter.insert(index + i, item.value)
            }
            selectionModel.select(item)
        }
    }

    override fun handle(command: String, source: Any): Boolean {
        when (command) {
            else -> return false
        }
        return true
    }
}

object NavHeader : BorderPane() {
    private val actions = listOf("newChapter", "renameChapter", "viewAttributes")

    init {
        styleClass += "nav-header"
        left = Label(tr("form.toc.title"), App.assets.graphicFor("tree/contents")).also {
            it.padding = Insets(0.0, 4.0, 0.0, 4.0)
            BorderPane.setAlignment(it, Pos.CENTER)
        }

        right = ToolBar().also {
            it.background = Background.EMPTY
            it.padding = Insets(2.0, 4.0, 2.0, 4.0)
            it.init(actions, Imabw, App, App.assets, Imabw.actionMap)
            BorderPane.setAlignment(it, Pos.CENTER)
        }
    }
}

object CellFactory : Callback<TreeView<Chapter>, TreeCell<Chapter>> {
    val book = App.assets.imageFor("tree/book")
    val section = App.assets.imageFor("tree/section")
    val chapter = App.assets.imageFor("tree/chapter")

    override fun call(param: TreeView<Chapter>): TreeCell<Chapter> {
        return ChapterCell()
    }
}

private class ChapterCell : TreeCell<Chapter>() {
    private val menu = ContextMenu()

    init {
        App.assets.resourceFor("ui/toc.idj")?.openStream()?.use { stream ->
            arrayListOf<Item>().apply {
                init(JSONArray(JSONTokener(stream)))
                menu.init(this, Imabw, App, App.assets, Imabw.actionMap, null)
            }
        }
    }

    override fun updateItem(chapter: Chapter?, empty: Boolean) {
        super.updateItem(chapter, empty)
        text = chapter?.title
        graphic = when {
            empty || chapter == null -> null
            chapter is Book -> ImageView(CellFactory.book)
            chapter.isSection -> ImageView(CellFactory.section)
            else -> ImageView(CellFactory.chapter)
        }
        tooltip = chapter?.intro?.toString()?.takeIf(String::isNotEmpty)?.let { intro ->
            Tooltip(intro).also {
                it.isWrapText = true
                it.maxWidthProperty().bind(widthProperty())
            }
        }
        contextMenu = chapter?.let { menu }
    }
}
