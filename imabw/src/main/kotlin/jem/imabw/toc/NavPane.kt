package jem.imabw.toc

import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.Background
import javafx.scene.layout.BorderPane
import javafx.util.Callback
import jem.Book
import jem.Chapter
import jem.imabw.Imabw
import jem.imabw.Work
import jem.imabw.Workbench
import jem.intro
import jem.title
import mala.App
import mala.App.tr
import mala.ixin.Item
import mala.ixin.graphicFor
import mala.ixin.imageFor
import mala.ixin.init
import org.json.JSONArray
import org.json.JSONTokener

object NavPane : BorderPane() {
    private val root = TreeItem<Chapter>()
    private val treeView = TreeView(root)
    private val navFooter = Label()

    init {
        top = NavHeader
        center = treeView
        bottom = navFooter

        treeView.isShowRoot = false
        treeView.cellFactory = CellFactory
        treeView.selectionModel.selectionMode = SelectionMode.MULTIPLE
        treeView.selectionModel.selectedItems.addListener(ListChangeListener {
            val list = it.list
            if (list.size < 2) {
                val chapter = list.first().value
                navFooter.text = "${chapter.size} sub-chapter(s)"
            } else {
                navFooter.text = "${list.size} selection(s)"
            }
        })

        navFooter.padding = Insets(2.0, 4.0, 2.0, 4.0)

        Workbench.tasks.addListener(ListChangeListener<Work> {
            while (it.next()) {
                if (it.wasAdded()) {
                    root.children += it.addedSubList.map { createNode(it.book) }
                } else if (it.wasRemoved()) {
                    println("removed")
                }
            }
        })
    }

    private fun createNode(chapter: Chapter): TreeItem<Chapter> {
        return object : TreeItem<Chapter>(chapter) {
            private var isFirstTime = true

            override fun isLeaf() = !value.isSection

            override fun getChildren(): ObservableList<TreeItem<Chapter>> {
                if (isFirstTime) {
                    isFirstTime = false
                    super.getChildren() += buildChildren(this)
                }
                return super.getChildren()
            }

            fun buildChildren(item: TreeItem<Chapter>): ObservableList<TreeItem<Chapter>> {
                val items = FXCollections.observableArrayList<TreeItem<Chapter>>()
                for (c in item.value) {
                    items += createNode(c)
                }
                return items
            }
        }
    }
}

object NavHeader : BorderPane() {
    private val actions = listOf("newChapter", "renameChapter", "viewAttributes")

    init {
        left = Label(tr("form.toc.title"), App.assets.graphicFor("tree/contents")).also {
            BorderPane.setAlignment(it, Pos.CENTER)
            it.padding = Insets(0.0, 4.0, 0.0, 4.0)
        }

        right = ToolBar().also {
            BorderPane.setAlignment(it, Pos.CENTER)
            it.background = Background.EMPTY
            it.padding = Insets(2.0, 4.0, 2.0, 4.0)
            it.init(actions, Imabw, App, App.assets, Imabw.actionMap)
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

class ChapterCell : TreeCell<Chapter>() {
    private val menu = ContextMenu()

    init {
        App.assets.resourceFor("ui/toc.idj")?.openStream()?.use {
            menu.init(arrayListOf<Item>().apply { init(JSONArray(JSONTokener(it))) }, Imabw, App, App.assets, Imabw.actionMap, null)
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
        tooltip = chapter?.intro?.toString()?.takeIf(String::isNotEmpty)?.let {
            Tooltip(it).also {
                it.isWrapText = true
                it.maxWidthProperty().bind(widthProperty())
            }
        }
        if (chapter != null) {
            contextMenu = menu
        }
    }
}
