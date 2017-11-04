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
import javafx.beans.binding.StringBinding
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Orientation
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import jclp.TypeManager
import jclp.ValueMap
import jclp.io.Flob
import jclp.io.flobOf
import jclp.log.Log
import jclp.release
import jclp.retain
import jclp.text.Text
import jclp.text.textOf
import jem.imabw.KeyAndName
import jem.imabw.LocalePicker
import mala.App
import mala.App.tr
import mala.ixin.getValue
import mala.ixin.graphicFor
import mala.ixin.init
import mala.ixin.setValue
import org.controlsfx.control.ListSelectionView
import org.fxmisc.wellbehaved.event.EventPattern
import org.fxmisc.wellbehaved.event.InputMap
import org.fxmisc.wellbehaved.event.Nodes
import java.time.LocalDate
import java.util.*
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

        override fun computeValue() = TypeManager.getType(value)
    }
    internal val typeNameProperty = object : StringBinding() {
        init {
            bind(typeProperty)
        }

        override fun dispose() {
            unbind(typeProperty)
        }

        override fun computeValue() = TypeManager.getName(typeProperty.value)
    }

    var key by keyProperty
    var value by valueProperty
}

open class VariantPane(val map: ValueMap, showName: Boolean = true) : BorderPane() {
    private val newValues = LinkedList<Any>()
    val data = FXCollections.observableArrayList<VariantItem>()
    val table = TableView<VariantItem>(data)
    val toolbar = ToolBar()

    var isModified = false
        private set

    private val selectedItem get() = table.selectionModel.selectedItem

    private val selectedItems get() = table.selectionModel.selectedItems

    private val currentKeys get() = data.map { it.key!! }

    init {
        center = table
        right = toolbar
        styleClass += "variant-pane"
        initTable(showName)
        initToolBar()
        initData()
    }

    fun syncVariants() {
        data.forEach { it.value.retain() }
        for (name in map.names.toList() - ignoredKeys()) {
            map.remove(name)
        }
        for (item in data) {
            item.keyProperty.value.takeIf { it.isNotEmpty() }?.let {
                map[it] = item.valueProperty.value
            }
        }
        data.forEach { it.value.release() }
        newValues.forEach { it.release() }
    }

    protected open fun getItemName(key: String) = key

    protected open fun getDefaultValue(key: String): Any = ""

    protected open fun getItemType(key: String): String? = null

    protected open fun ignoredKeys(): Collection<String> = emptyList()

    protected open fun availableKeys(): Collection<String> = emptyList()

    protected open fun toString(value: Any) = TypeManager.printable(value)

    protected open fun newDialogTitle() = tr("d.newVariant.title")

    private fun initTable(showName: Boolean) {
        with(table) {
            isEditable = true
            selectionModel.selectionMode = SelectionMode.MULTIPLE
            columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
            Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.DELETE), {
                removeItem(selectedItems, true)
            }))
            Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.INSERT), {
                newItem()
            }))
            columns += TableColumn<VariantItem, String>(tr("com.variantPane.id")).apply {
                setCellValueFactory { it.value.keyProperty }
            }
            if (showName) {
                columns += TableColumn<VariantItem, String>(tr("com.variantPane.name")).apply {
                    setCellValueFactory {
                        Bindings.createStringBinding(Callable { getItemName(it.value.key!!) }, it.value.keyProperty)
                    }
                }
            }
            columns += TableColumn<VariantItem, String>(tr("com.variantPane.type")).apply {
                setCellValueFactory { it.value.typeNameProperty }
            }
            columns += TableColumn<VariantItem, String>(tr("com.variantPane.value")).apply {
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
                val exportableTypes = arrayOf(TypeManager.FLOB, TypeManager.TEXT)
                disableProperty().bind(Bindings.createBooleanBinding(Callable {
                    selection.size != 1 || selection.first().typeProperty.value !in exportableTypes
                }, selection))
            }
        }
    }

    private fun newButton(id: String): Button {
        val button = Button("", App.assets.graphicFor("misc/$id"))
        button.tooltip = Tooltip(tr("com.variantPane.$id.toast"))
        return button
    }

    private fun initData() {
        map.filter { it.key !in ignoredKeys() }.mapTo(data) { VariantItem(it.key, it.value) }
        table.selectionModel.select(0)
    }

    private fun newItem() {
        val items = arrayListOf<KeyAndName>()
        for (key in availableKeys() - ignoredKeys() - currentKeys) {
            items += KeyAndName(key, getItemName(key))
        }
        if (items.isNotEmpty()) {
            newPredefinedItem(items)
        } else {
            newCustomizedItem()
        }
    }

    private fun newPredefinedItem(items: Collection<KeyAndName>) {
        with(Dialog<ButtonType>()) {
            init(newDialogTitle(), scene.window, "newVariant1")
            val selectionView = ListSelectionView<KeyAndName>().apply {
                sourceItems.addAll(items)
                dialogPane.content = this
            }
            val customize = ButtonType(tr("d.newVariant.customize"), ButtonBar.ButtonData.LEFT)
            dialogPane.buttonTypes.setAll(ButtonType.OK, ButtonType.CANCEL, customize)
            dialogPane.lookupButton(ButtonType.OK).disableProperty().bind(Bindings.isEmpty(selectionView.targetItems))
            val buttonType = showAndWait().get()
            if (buttonType == ButtonType.OK) {
                val model = table.selectionModel.apply { clearSelection() }
                for (item in selectionView.targetItems) {
                    data += VariantItem(item.key, getDefaultValue(item.key).also { newValues += it })
                    model.selectLast()
                    isModified = true
                }
                table.scrollTo(table.items.size - 1)
            } else if (buttonType == customize) {
                newCustomizedItem()
            }
        }
    }

    private fun newCustomizedItem() {
        val invalidKeys = currentKeys + ignoredKeys()
        with(Dialog<ButtonType>()) {
            init(newDialogTitle(), scene.window, "newVariant2")
            dialogPane.buttonTypes.setAll(ButtonType.OK, ButtonType.CANCEL)
            val okButton = dialogPane.lookupButton(ButtonType.OK)
            val nameField = TextField().apply {
                prefWidth = minOf(400.0, this@VariantPane.width * 0.5)
                textProperty().addListener { _, _, text ->
                    okButton.isDisable = text.isEmpty() || text in invalidKeys
                }
            }
            val typeCombo = ComboBox<KeyAndName>().apply {
                prefWidth = minOf(400.0, this@VariantPane.width * 0.5)
                items.addAll(TypeManager.allTypes.map { KeyAndName(it, TypeManager.getName(it) ?: it.capitalize()) })
                selectionModel.select(0)
            }
            dialogPane.content = GridPane().apply {
                vgap = 10.0
                hgap = 10.0
                init(listOf(
                        Label(tr("d.newVariant.name")), Label(tr("d.newVariant.type"))
                ), listOf(
                        nameField, typeCombo
                ))
            }
            nameField.requestFocus()
            okButton.isDisable = true
            if (showAndWait().get() == ButtonType.OK) {
                val value = TypeManager.getDefault(typeCombo.selectionModel.selectedItem.key)
                data += VariantItem(nameField.text.trim(), value?.also { newValues += it } ?: "")
                table.selectionModel.apply { clearSelection() }.selectLast()
                table.scrollTo(table.items.size - 1)
                isModified = true
            }
        }
    }

    private fun removeItem(items: Collection<VariantItem>, showWarn: Boolean = false) {
        if (items.isEmpty()) {
            return
        }
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
        val value = item.value
        if (value is Flob) {

        } else if (value is Text) {

        }
    }

    private inner class ValueCell<S, T : Any> : TableCell<S, T>() {
        init {
            isWrapText = false
        }

        override fun updateItem(item: T?, empty: Boolean) {
            super.updateItem(item, empty)
            text = item?.toString()
        }

        @Suppress("UNCHECKED_CAST")
        override fun startEdit() {
            super.startEdit()
            val item = selectedItem
            when (item.typeProperty.value) {
                TypeManager.BOOLEAN -> {
                    item.value = !(item.value as Boolean)
                    isModified = true
                    cancelEdit()
                }
                TypeManager.DATE -> {
                    graphic = DatePicker(item.value as LocalDate).apply {
                        isShowWeekNumbers = true
                        setOnAction { commitEdit(value as T) }
                    }
                }
                TypeManager.LOCALE -> {
                    graphic = LocalePicker(item.value as Locale).apply {
                        setOnAction { commitEdit(value as T) }
                    }
                }
                TypeManager.TEXT -> {
                    longText(tr("d.editVariant.title", getItemName(item.key!!)), item.value.toString(), owner = scene.window)?.let {
                        item.value = textOf(it)
                        isModified = true
                    }
                    cancelEdit()
                }
                TypeManager.FLOB -> {
                    selectFile(tr("d.selectFile.title"), scene.window)?.let {
                        item.value = flobOf(it.toPath())
                        isModified = true
                    }
                    cancelEdit()
                }
                else -> {
                    graphic = TextField(item.value.toString()).apply {
                        setOnAction { commitEdit(text as T) }
                    }
                }
            }
            graphic?.apply {
                prefWidth(this@ValueCell.width - 8.0)
                requestFocus()
            }
        }

        override fun commitEdit(value: T) {
            graphic = null
            tableView.requestFocus()
            val item = selectedItem as VariantItem
            item.value = if (value is String) {
                try {
                    TypeManager.parse(item.typeProperty.value, value) ?: return
                } catch (e: Exception) {
                    Log.e("editVariant", e) { "bad input string for ${item.key}" }
                    return
                }
            } else {
                value
            }
            isModified = true
            super.commitEdit(value)
        }

        override fun cancelEdit() {
            super.cancelEdit()
            graphic = null
        }
    }
}
