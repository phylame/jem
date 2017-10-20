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

package jem.imabw.ui

import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.binding.StringBinding
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Orientation
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import jclp.VariantMap
import jclp.Variants
import jem.Attributes
import mala.App
import mala.App.tr
import mala.ixin.getValue
import mala.ixin.graphicFor
import mala.ixin.setValue
import org.controlsfx.control.PopOver
import org.fxmisc.wellbehaved.event.EventPattern
import org.fxmisc.wellbehaved.event.InputMap
import org.fxmisc.wellbehaved.event.Nodes
import java.util.concurrent.Callable

class VariantItem(key: String, value: Any) {
    internal val keyProperty = SimpleStringProperty(key)
    internal val valueProperty = SimpleObjectProperty(value)
    internal val typeProperty = object : StringBinding() {
        init {
            bind(valueProperty)
        }

        override fun dispose() {
            unbind(valueProperty)
        }

        override fun computeValue() = Variants.getType(value)
    }
    internal val typeNameProperty = object : StringBinding() {
        init {
            bind(typeProperty)
        }

        override fun dispose() {
            unbind(typeProperty)
        }

        override fun computeValue() = Variants.getName(typeProperty.value)
    }

    var key by keyProperty
    var value by valueProperty
}

open class VariantPane(val map: VariantMap) : BorderPane() {
    val exportableTypes = mutableSetOf(Variants.FLOB, Variants.TEXT)
    val data = FXCollections.observableArrayList<VariantItem>()
    val table = TableView<VariantItem>(data)
    val toolbar = ToolBar()

    var isModified = false
        private set

    private val selectedItem get() = table.selectionModel.selectedItem

    private val selectedItems get() = table.selectionModel.selectedItems

    init {
        center = table
        right = toolbar
        styleClass += "variant-pane"
        initTable()
        initToolBar()
        initData()
    }

    fun syncToSource() {
        for (name in map.names.toList() - ignoredKeys()) {
            map.remove(name)
        }
        for (item in data) {
            item.keyProperty.value.takeIf { it.isNotEmpty() }?.let {
                map[it] = item.valueProperty.value
            }
        }
    }

    protected open fun getItemType(key: String): String? = null

    protected open fun getItemName(key: String) = key.capitalize()

    protected open fun ignoredKeys(): Collection<String> = emptyList()

    protected open fun availableKeys(): Collection<String> = emptyList()

    protected open fun toString(value: Any) = value.toString()

    private fun initTable() {
        with(table) {
            isEditable = true
            selectionModel.selectionMode = SelectionMode.MULTIPLE
            Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.DELETE), {
                if (selectedItems.isNotEmpty()) removeItem(selectedItems, true)
            }))
            columns += TableColumn<VariantItem, String>(tr("d.editVariant.id")).apply {
                setCellValueFactory { it.value.keyProperty }
            }
            columns += TableColumn<VariantItem, String>(tr("d.editVariant.name")).apply {
                setCellValueFactory {
                    Bindings.createStringBinding(Callable { getItemName(it.value.key!!) }, it.value.keyProperty)
                }
            }
            columns += TableColumn<VariantItem, String>(tr("d.editVariant.type")).apply {
                setCellValueFactory { it.value.typeNameProperty }
            }
            columns += TableColumn<VariantItem, String>(tr("d.editVariant.value")).apply {
                setCellFactory { ValueCell() }
                setCellValueFactory {
                    Bindings.createStringBinding(Callable { toString(it.value.value!!) }, it.value.valueProperty)
                }
            }
        }
    }

    private fun initToolBar() {
        val selection = selectedItems
        with(toolbar) {
            orientation = Orientation.VERTICAL
            items += newButton("create").apply {
                setOnAction { newItem() }
            }
            items += newButton("remove").apply {
                setOnAction { removeItem(selection) }
                disableProperty().bind(Bindings.isEmpty(selection))
            }
            items += newButton("export").apply {
                setOnAction { exportItem(selectedItem) }
                disableProperty().bind(Bindings.createBooleanBinding(Callable {
                    selection.size != 1 || selection.first().typeProperty.value !in exportableTypes
                }, selection))
            }
        }
    }

    private fun newButton(id: String): Button {
        val button = Button("", App.assets.graphicFor("misc/$id"))
        button.tooltip = Tooltip(tr("d.editVariant.$id.toast"))
        return button
    }

    private fun initData() {
        map.filter { it.first !in ignoredKeys() }.mapTo(data) { VariantItem(it.first, it.second) }
        Platform.runLater {
            table.requestFocus()
            table.selectionModel.select(0)
        }
    }

    private fun newItem() {
        val items = arrayListOf<KeyAndName>()
        for (key in availableKeys() - ignoredKeys() - data.map { it.key!! }) {
            items += KeyAndName(key, getItemName(key))
        }
        items += KeyAndName("", tr("d.newVariant.customized"))
        val stage = scene.window as Stage
        val popOver = PopOver().apply {
            isHeaderAlwaysVisible = true
            title = tr("d.newVariant.title")
            contentNode = ListView<KeyAndName>().also {
                it.items.addAll(items)
                it.selectionModel.selectedItemProperty().addListener { _, _, item ->
                    hide()
                    if (getItemType(item.key) != null) {
                        data += VariantItem(item.key, Attributes.getDefault(item.key) ?: "")
                        table.selectionModel.clearAndSelect(data.size - 1)
                        isModified = true
                    }else{
                        input("","","",stage)?.let {

                        }
                    }
                }
            }
        }
        stage.setOnCloseRequest {
            popOver.hide()
            if (popOver.isShowing) {
                it.consume()
            }
        }
        popOver.show(toolbar.items.first())
//        if (items.isEmpty()) {
//            data += VariantItem("", "")
//            table.selectionModel.clearAndSelect(data.size - 1)
//            return
//        }
//        with(ChoiceDialog(items.first(), items)) {
//            init(tr("d.newVariant.title"), scene.window)
//            showAndWait().takeIf { it.isPresent }?.get()?.key?.let {
//                data += VariantItem(it, Attributes.getDefault(it) ?: "")
//                table.selectionModel.clearAndSelect(data.size - 1)
//                isModified = true
//            }
//        }
    }

    private fun removeItem(items: Collection<VariantItem>, showWarn: Boolean = false) {
        require(items.isNotEmpty()) { "no selected items" }
        if (showWarn) {
            val hint = if (items.size == 1) {
                tr("d.removeVariant.oneHint", getItemName(items.first().keyProperty.value))
            } else {
                tr("d.removeVariant.moreHint", items.size)
            }
            if (!confirm(tr("d.removeVariant.title"), hint)) {
                return
            }
        }
        for (item in items) {
            data.remove(item)
            isModified = true
        }
    }

    private fun exportItem(item: VariantItem) {
    }

    private class KeyAndName(val key: String, val name: String) {
        override fun toString() = name
    }

    private inner class ValueCell<S, T : Any> : TableCell<S, T>() {
        override fun updateItem(item: T?, empty: Boolean) {
            super.updateItem(item, empty)
            text = item?.toString()
        }

        @Suppress("UNCHECKED_CAST")
        override fun startEdit() {
            super.startEdit()
            val item = tableView.selectionModel.selectedItem as VariantItem
            when (item.typeProperty.value) {
                Variants.STRING, Variants.INTEGER, Variants.REAL -> {
                    graphic = TextField(item.value.toString()).apply {
                        prefWidth = this@ValueCell.width
                        setOnAction { commitEdit(text as T) }
                        Platform.runLater { requestFocus() }
                    }
                }
            }
        }

        override fun commitEdit(text: T) {
            graphic = null
            super.commitEdit(text)
            tableView.requestFocus()
            val item = tableView.selectionModel.selectedItem as VariantItem
            when (item.typeProperty.value) {
                Variants.STRING -> item.value = text
                Variants.INTEGER -> try {
                    item.value = text.toString().toInt()
                } catch (e: NumberFormatException) {
                    this.text = toString(item.value!!)
                    return
                }
                Variants.REAL -> try {
                    item.value = text.toString().toDouble()
                } catch (e: NumberFormatException) {
                    this.text = toString(item.value!!)
                    return
                }
            }
            isModified = true
        }

        override fun cancelEdit() {
            super.cancelEdit()
            graphic = null
        }
    }
}
