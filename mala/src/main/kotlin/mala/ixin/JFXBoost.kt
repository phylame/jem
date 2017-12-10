/*
 * Copyright 2015-2017 Peng Wan <phylame@163.com>
 *
 * This file is part of Jem.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mala.ixin

import javafx.application.Application
import javafx.beans.binding.ObjectBinding
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableBooleanValue
import javafx.beans.value.WritableValue
import javafx.collections.ObservableList
import javafx.geometry.HPos
import javafx.geometry.VPos
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.Separator
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import jclp.HierarchySupport
import jclp.text.ifNotEmpty
import kotlin.reflect.KProperty

val MouseEvent.isPrimary
    inline get() = button == MouseButton.PRIMARY

val MouseEvent.isDoubleClick
    inline get() = clickCount == 2

fun ComboBox<*>.selectPreviousOrLast() {
    with(selectionModel) {
        if (selectedIndex == 0) {
            selectLast()
        } else {
            selectPrevious()
        }
    }
}

fun ComboBox<*>.selectNextOrFirst() {
    with(selectionModel) {
        if (selectedIndex == items.lastIndex) {
            selectFirst()
        } else {
            selectNext()
        }
    }
}

fun Node.clearAnchorConstraints(top: Double = 0.0, right: Double = 0.0, bottom: Double = 0.0, left: Double = 0.0) {
    AnchorPane.setTopAnchor(this, top)
    AnchorPane.setRightAnchor(this, right)
    AnchorPane.setBottomAnchor(this, bottom)
    AnchorPane.setLeftAnchor(this, left)
}

operator fun Pane.plusAssign(node: Node) {
    children.add(node)
}

operator fun Pane.plusAssign(elements: Collection<Node>) {
    children.addAll(elements)
}

operator fun Group.plusAssign(node: Node) {
    children.add(node)
}

operator fun Group.plusAssign(elements: Collection<Node>) {
    children.addAll(elements)
}

fun GridPane.initAsForm(labels: Collection<Node>, fields: Collection<Node>, firstRow: Int = 0, vPos: VPos = VPos.CENTER) {
    vgap = 8.0
    hgap = 8.0
    val nodes = ArrayList<Node>(fields.size)
    labels.forEachIndexed { index, node ->
        nodes += node
        node.styleClass += "form-label"
        GridPane.setValignment(node, vPos)
        GridPane.setHalignment(node, HPos.RIGHT)
        add(node, 0, firstRow + index)
    }
    fields.forEachIndexed { index, node ->
        node.styleClass += "form-field"
        GridPane.setValignment(node, vPos)
        GridPane.setHalignment(node, HPos.LEFT)
        GridPane.setHgrow(node, Priority.ALWAYS)
        add(node, 1, firstRow + index)
    }
}

const val SEPARATOR_LABEL = "-*----*-"

fun GridPane.initAsInfo(items: Iterator<Any>, app: Application) {
    vgap = 8.0
    hgap = 16.0
    var row = 0
    var isLabel = true
    while (items.hasNext()) {
        val item = items.next()
        when {
            item == SEPARATOR_LABEL -> add(Separator(), 0, row++, 2, 1)
            isLabel -> {
                Label("$item:").let {
                    GridPane.setValignment(it, VPos.CENTER)
                    GridPane.setHalignment(it, HPos.RIGHT)
                    add(it, 0, row)
                }
                isLabel = false
            }
            else -> {
                val text = item.toString()
                if (text.matches("\\w+://.*".toRegex())) {
                    Hyperlink(text).apply {
                        setOnAction { app.hostServices.showDocument(text) }
                    }
                } else {
                    Label(text)
                }.let {
                    GridPane.setValignment(it, VPos.CENTER)
                    GridPane.setHalignment(it, HPos.LEFT)
                    add(it, 1, row++)
                }
                isLabel = true
            }
        }
    }
}

fun <T : HierarchySupport<T>> T.toTreeItem(): TreeItem<T> = object : TreeItem<T>(this) {
    private var isFirstTime = true

    override fun isLeaf() = value.size == 0

    override fun getChildren(): ObservableList<TreeItem<T>> {
        if (isFirstTime) {
            isFirstTime = false
            super.getChildren() += value.map { it.toTreeItem() }
        }
        return super.getChildren()
    }
}

fun <T> TreeView<T>.resetSelection(): List<TreeItem<T>> =
        with(selectionModel) {
            selectedItems.toList().apply { clearSelection() }
        }

fun <T> TreeView<T>.selectAndScrollTo(item: TreeItem<T>) {
    with(selectionModel) {
        clearSelection()
        select(item)
        scrollTo(selectedIndex)
    }
}

fun <T> TreeItem<T>.mostBelow(top: TreeItem<T>? = null): TreeItem<T> {
    var parent: TreeItem<T> = this
    while (parent.parent !== top) {
        parent = parent.parent!!
    }
    return parent
}

val TreeItem<*>.isRoot inline get() = parent == null

val TreeItem<*>.isNotRoot inline get() = parent != null

fun TreeItem<*>.refresh() {
    value.let {
        value = null
        value = it
    }
}

operator fun WritableBooleanValue.getValue(ref: Any, property: KProperty<*>): Boolean {
    return value
}

operator fun WritableBooleanValue.setValue(ref: Any, property: KProperty<*>, value: Boolean) {
    this.value = value
}

operator fun <T> WritableValue<T>.getValue(ref: Any, property: KProperty<*>): T? {
    return value
}

operator fun <T> WritableValue<T>.setValue(ref: Any, property: KProperty<*>, value: T?) {
    this.value = value
}

fun ObservableValue<Image?>.lazyImageView() = object : ObjectBinding<ImageView>() {
    init {
        bind(this@lazyImageView)
    }

    override fun dispose() {
        unbind(this@lazyImageView)
    }

    override fun computeValue(): ImageView? {
        return this@lazyImageView.value?.let { ImageView(it) }
    }
}

fun ObservableValue<String?>.lazyTooltip() = object : ObjectBinding<Tooltip>() {
    init {
        bind(this@lazyTooltip)
    }

    override fun dispose() {
        unbind(this@lazyTooltip)
    }

    override fun computeValue(): Tooltip? {
        return this@lazyTooltip.value?.ifNotEmpty { Tooltip(it) }
    }
}
