/*
 * Copyright 2017 Peng Wan <phylame@163.com>
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

import javafx.beans.binding.ObjectBinding
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import javafx.collections.ObservableList
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.Tooltip
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import jclp.Hierarchy
import kotlin.reflect.KProperty

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

fun <T : Hierarchy<T>> T.toTreeItem(): TreeItem<T> = object : TreeItem<T>(this) {
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

val TreeItem<*>.isRoot get() = parent == null

val TreeItem<*>.isNotRoot get() = parent != null

fun TreeItem<*>.refresh() {
    value.let {
        value = null
        value = it
    }
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
        return this@lazyTooltip.value?.takeIf { it.isNotEmpty() }?.let { Tooltip(it) }
    }
}
