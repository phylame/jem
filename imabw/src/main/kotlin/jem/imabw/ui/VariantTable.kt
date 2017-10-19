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
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Orientation
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.util.StringConverter
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

    protected open fun isEditable(item: VariantItem) = true

    protected open fun getItemName(key: String) = key.capitalize()

    protected open fun getItemType(key: String) = Variants.STRING

    protected open fun ignoredNames(): Collection<String> = emptyList()

    protected open fun availableNames(): Collection<String> = emptyList()

    protected open fun toString(value: Any) = value.toString()

    private fun initTable() {
        with(table) {
            isEditable = true
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
                setCellValueFactory { Bindings.createStringBinding(Callable { toString(it.value.value.value) }, it.value.value) }
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
        map.filter { it.first !in ignoredNames() }.mapTo(data) { (id, value) ->
            VariantItem(id, getItemName(id), value)
        }
        table.selectionModel.select(0)
        Platform.runLater { table.requestFocus() }
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
        require(items.isNotEmpty()) { "no selected items" }
        if (showWarn) {
            val hint = if (items.size == 1) {
                tr("d.removeVariant.oneHint", items.first().name.value)
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

    private inner class NewItemDialog : Dialog<ButtonType>() {
        private val names = ComboBox<KeyedItem>()
        private val types = ComboBox<KeyedItem>()
        private lateinit var defaultTypeItem: KeyedItem

        lateinit var item: VariantItem

        init {
            val pane = GridPane()
            pane.hgap = 8.0
            pane.vgap = 8.0
            pane.init(listOf(
                    Label(tr("d.newVariant.name")),
                    Label(tr("d.newVariant.type"))
            ), listOf(
                    names.apply { initNames(this) },
                    types.apply { initTypes(this) }
            ))
            dialogPane.content = pane
            init(tr("d.newVariant.title"), scene.window)
            dialogPane.buttonTypes.setAll(ButtonType.OK, ButtonType.CANCEL)
            setOnCloseRequest {
                println(names.isEditable)
                val key = if (names.isEditable) names.editor.text else names.selectionModel.selectedItem.key
                println(key)
                item = VariantItem(key, "", Variants.getDefault(types.selectionModel.selectedItem.key)!!)
            }
            Platform.runLater { names.selectionModel.select(0) }
        }

        private fun initNames(nameCombo: ComboBox<KeyedItem>) {
            nameCombo.prefWidth = minOf(scene.width * 0.8, 360.0)
            nameCombo.converter = object : StringConverter<KeyedItem>() {
                override fun toString(item: KeyedItem?): String? = item?.name

                override fun fromString(name: String) = KeyedItem(name, getItemName(name))
            }
            for (key in availableNames() - ignoredNames() - data.map { it.key.value }) {
                nameCombo.items.add(KeyedItem(key, getItemName(key)))
            }
            nameCombo.items.add(KeyedItem("", tr("d.newVariant.customized")))
            nameCombo.selectionModel.selectedItemProperty().addListener { _, _, nameItem ->
                if (nameItem != null) {
                    if (nameItem.key.isEmpty()) {
                        names.isEditable = true
                        types.selectionModel.select(defaultTypeItem)
                    } else {
                        names.isEditable = false
                        val typeItem = types.items.find { it.key == getItemType(nameItem.key) }
                        if (typeItem != null) {
                            types.selectionModel.select(typeItem)
                            types.isDisable = true
                        } else {
                            types.selectionModel.select(defaultTypeItem)
                            types.isDisable = false
                        }
                    }
                } else {
                    names.isEditable = false
                }
            }
        }

        private fun initTypes(typeCombo: ComboBox<KeyedItem>) {
            typeCombo.prefWidth = minOf(scene.width * 0.8, 360.0)
            for (type in Variants.allTypes) {
                typeCombo.items.add(KeyedItem(type, Variants.getName(type) ?: type.capitalize()).also {
                    if (it.key == Variants.STRING) {
                        defaultTypeItem = it
                    }
                })
            }
        }
    }

    private class KeyedItem(val key: String, val name: String) {
        override fun toString() = name
    }
}
