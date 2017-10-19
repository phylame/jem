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

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Orientation
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import jclp.VariantMap
import jclp.Variants
import mala.App
import mala.App.tr
import mala.ixin.graphicFor
import mala.ixin.init
import org.fxmisc.wellbehaved.event.EventPattern
import org.fxmisc.wellbehaved.event.InputMap
import org.fxmisc.wellbehaved.event.Nodes
import java.util.concurrent.Callable

class VariantItem(key: String, name: String, value: Any) {
    val key = SimpleStringProperty(key)
    val name = SimpleStringProperty(name)
    val value = SimpleObjectProperty(value)
    val type = SimpleStringProperty(Variants.getType(value))
    val typeName = SimpleStringProperty(Variants.getName(type.value))
}

open class VariantPane(val map: VariantMap) : BorderPane() {
    val ignoredNames = mutableSetOf<String>()
    val exportableTypes = mutableSetOf(Variants.FLOB, Variants.TEXT)
    val data = FXCollections.observableArrayList<VariantItem>()
    val table = TableView<VariantItem>(data)
    val toolbar = ToolBar()

    var isModified = false
        private set

    private val selectedItem get() = table.selectionModel.selectedItem

    private val selectedItems get() = table.selectionModel.selectedItems

    init {
        initData()
        initTable()
        initToolBar()
        center = table
        right = toolbar
    }

    protected open fun isEditable(item: VariantItem) = true

    protected open fun getItemName(id: String) = id.capitalize()

    protected open fun getAvailableKeys(): Collection<String> {
        return emptyList()
    }

    private fun initTable() {
        with(table) {
            Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.DELETE), {
                if (selectedItems.isNotEmpty()) removeItem(selectedItems, true)
            }))
            Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER), {
                if (selectedItems.size == 1) editItem(selectedItems.first())
            }))
            selectionModel.selectionMode = SelectionMode.MULTIPLE
            columns += TableColumn<VariantItem, String>(tr("d.editVariant.id")).apply {
                setCellValueFactory { it.value.key }
            }
            columns += TableColumn<VariantItem, String>(tr("d.editVariant.name")).apply {
                setCellValueFactory { it.value.name }
            }
            columns += TableColumn<VariantItem, String>(tr("d.editVariant.type")).apply {
                setCellValueFactory { it.value.typeName }
            }
            columns += TableColumn<VariantItem, String>(tr("d.editVariant.value")).apply {
                setCellValueFactory { Bindings.convert(it.value.value) }
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
            items += newButton("edit").apply {
                setOnAction { editItem(selectedItem) }
                disableProperty().bind(Bindings.createBooleanBinding(Callable {
                    selection.size != 1 || !isEditable(selection.first())
                }, selection))
            }
            items += newButton("remove").apply {
                setOnAction { removeItem(selection) }
                disableProperty().bind(Bindings.isEmpty(selection))
            }
            items += newButton("export").apply {
                setOnAction { exportItem(selectedItem) }
                disableProperty().bind(Bindings.createBooleanBinding(Callable {
                    selection.size != 1 || selection.first().type.value !in exportableTypes
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
        map.filter { it.first !in ignoredNames }.mapTo(data) { (id, value) ->
            VariantItem(id, getItemName(id), value)
        }
    }

    private fun newItem() {
        with(NewItemDialog()) {
            if (showAndWait().get() == ButtonType.OK) {
                data += item
                isModified = true
            }
        }
    }

    private fun editItem(item: VariantItem) {
    }

    private fun removeItem(items: Collection<VariantItem>, showWarn: Boolean = false) {
        if (!showWarn || confirm("Remove", "Are you sure?")) {
            for (item in items) {
                data.remove(item)
            }
        }
    }

    private fun exportItem(item: VariantItem) {
    }

    private inner class NewItemDialog : Dialog<ButtonType>() {
        lateinit var item: VariantItem

        init {
            val box = VBox()
            val pane = GridPane()
            pane.hgap = 8.0
            pane.vgap = 8.0
            pane.init(listOf(
                    Label("Name:"),
                    Label("Type:")
            ), listOf(
                    ComboBox<KeyedItem>().apply { initNames(this) },
                    ComboBox<KeyedItem>().apply { initTypes(this) }
            ))
            box.children += pane

            dialogPane.content = box
            init("New Item", scene.window)
            dialogPane.buttonTypes.setAll(ButtonType.OK, ButtonType.CANCEL)
        }

        private fun initNames(comboBox: ComboBox<KeyedItem>) {
            for (key in getAvailableKeys() - data.map { it.key.value }) {
                comboBox.items.add(KeyedItem(key, getItemName(key)))
            }
            comboBox.selectionModel.selectFirst()
        }

        private fun initTypes(comboBox: ComboBox<KeyedItem>) {
            for (type in Variants.allTypes) {
                comboBox.items.add(KeyedItem(type, Variants.getName(type) ?: type.capitalize()))
            }
            comboBox.selectionModel.selectFirst()
        }
    }

    private class KeyedItem(val id: String, val name: String) {
        override fun toString() = name
    }
}
