package jem.imabw.toc

import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.util.Callback
import jem.Book
import jem.Chapter
import jem.imabw.Imabw
import jem.imabw.Task
import jem.imabw.Workbench
import jem.title
import mala.App
import mala.App.tr
import mala.ixin.graphicFor
import mala.ixin.imageFor
import mala.ixin.plusAssign
import mala.ixin.toButton

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

        

        Workbench.tasks.addListener(ListChangeListener<Task> {
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
    private val actions = arrayOf("newChapter", "renameChapter", "viewAttributes")

    init {
        left = Label(tr("form.toc.title"), App.assets.graphicFor("tree/contents"))

        val toolBar = HBox()
        toolBar.spacing = 5.0
        for (action in actions) {
            toolBar += Imabw.newAction(action).toButton(Imabw, hideText = true)
        }
        right = toolBar
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
    override fun updateItem(item: Chapter?, empty: Boolean) {
        super.updateItem(item, empty)
        text = item?.title
        graphic = when {
            empty || item == null -> null
            item is Book -> ImageView(CellFactory.book)
            item.isSection -> ImageView(CellFactory.section)
            else -> ImageView(CellFactory.chapter)
        }
    }
}
