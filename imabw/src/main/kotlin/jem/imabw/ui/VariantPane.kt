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
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.Region
import jclp.TypeManager
import jclp.ValueMap
import jclp.io.Flob
import jclp.io.flobOf
import jclp.release
import jclp.retain
import jclp.text.Text
import jclp.text.or
import jclp.text.textOf
import jem.imabw.*
import mala.App
import mala.App.tr
import mala.ixin.graphicFor
import mala.ixin.initAsForm
import mala.ixin.selectNextOrFirst
import mala.ixin.selectPreviousOrLast
import org.controlsfx.control.ListSelectionView
import java.time.LocalDate
import java.util.*
import java.util.concurrent.Callable

class VariantItem(key: String, data: Any) {
    internal val keyProperty = SimpleStringProperty(key)

    internal val dataProperty = SimpleObjectProperty(data)

    internal val typeProperty = Bindings.createStringBinding(Callable {
        TypeManager.getType(dataProperty.value) ?: ""
    }, dataProperty)

    val key: String get() = keyProperty.value

    val type: String get() = typeProperty.value

    var data: Any
        get() = dataProperty.value
        set(value) {
            dataProperty.value = value
        }
}

open class VariantPane(val map: ValueMap, val tag: String, showName: Boolean = true) : BorderPane() {
    private val newValues = LinkedList<Any>()

    val variants = FXCollections.observableArrayList<VariantItem>()

    val table = TableView<VariantItem>(variants)

    val toolbar = ToolBar()

    var isModified = false
        private set

    private val selectedItem get() = table.selectionModel.selectedItem

    private val selectedItems get() = table.selectionModel.selectedItems

    private val currentKeys get() = variants.map { it.key }

    init {
        center = table
        right = toolbar
        styleClass += "variant-pane"

        initTable(showName)
        initToolBar()
        initData()
    }

    fun syncVariants() {
        variants.forEach { it.data.retain() }
        for (name in map.names.toList() - ignoredKeys()) {
            map.remove(name)
        }
        for (item in variants) {
            item.key.takeIf { it.isNotEmpty() }?.let {
                map[it] = item.data
            }
        }
        variants.forEach { it.data.release() }
        newValues.forEach { it.release() }
    }

    fun storeState() {
        UISettings.store(table, tag)
    }

    protected open fun getItemTitle(key: String): String = key

    protected open fun getTypeTitle(type: String): String = TypeManager.getTitle(type) or type

    protected open fun formatItemData(value: Any): String = TypeManager.printable(value) or { value.toString() }

    protected open fun getItemType(key: String): String? = null

    protected open fun getDefaultValue(key: String): Any = ""

    protected open fun getAvailableValues(key: String): List<String> = emptyList()

    protected open fun availableKeys(): Collection<String> = emptyList()

    protected open fun ignoredKeys(): Collection<String> = emptyList()

    protected open fun dialogNewTitle() = tr("d.newVariant.title")

    private fun initTable(showTitle: Boolean) {
        with(table) {
            isEditable = true
            selectionModel.selectionMode = SelectionMode.MULTIPLE
            columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
            addEventHandler(KeyEvent.KEY_PRESSED) {
                if (it.code == KeyCode.DELETE) {
                    removeItem(selectedItems, true)
                }
            }
            addEventHandler(KeyEvent.KEY_PRESSED) {
                if (it.code == KeyCode.INSERT) {
                    newItem()
                }
            }
            columns += TableColumn<VariantItem, String>(tr("com.variant.id")).apply {
                setCellValueFactory { it.value.keyProperty }
            }
            if (showTitle) {
                columns += TableColumn<VariantItem, String>(tr("com.variant.name")).apply {
                    setCellValueFactory {
                        Bindings.createStringBinding(Callable { getItemTitle(it.value.key) }, it.value.keyProperty)
                    }
                }
            }
            columns += TableColumn<VariantItem, String>(tr("com.variant.type")).apply {
                setCellValueFactory {
                    Bindings.createStringBinding(Callable { getTypeTitle(it.value.type) }, it.value.typeProperty)
                }
            }
            columns += TableColumn<VariantItem, String>(tr("com.variant.value")).apply {
                setCellFactory { EditableDataCell() }
                setCellValueFactory {
                    Bindings.createStringBinding(Callable { formatItemData(it.value.data) }, it.value.dataProperty)
                }
            }
            UISettings.restore(this, tag)
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
                    selection.size != 1 || selection.first().type !in exportableTypes
                }, selection))
            }
        }
    }

    private fun newButton(id: String): Button {
        val button = Button("", App.assets.graphicFor("misc/$id"))
        button.tooltip = Tooltip(tr("com.variant.$id.toast"))
        return button
    }

    private fun initData() {
        map.filter { it.key !in ignoredKeys() }.mapTo(variants) { VariantItem(it.key, it.value) }
        table.selectionModel.select(0)
    }

    private fun newItem() {
        val items = arrayListOf<KeyAndName>()
        for (key in availableKeys() - ignoredKeys() - currentKeys) {
            items += KeyAndName(key, getItemTitle(key))
        }
        if (items.isNotEmpty()) {
            newPredefinedItem(items)
        } else {
            newCustomizedItem()
        }
    }

    private fun newPredefinedItem(items: Collection<KeyAndName>) {
        with(Dialog<ButtonType>()) {
            init(dialogNewTitle(), scene.window, "newVariant1")
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
                    variants += VariantItem(item.key, getDefaultValue(item.key).also { newValues += it })
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
            init(dialogNewTitle(), scene.window, "newVariant2")
            dialogPane.buttonTypes.setAll(ButtonType.OK, ButtonType.CANCEL)
            val okButton = dialogPane.lookupButton(ButtonType.OK)
            val typeCombo = ComboBox<KeyAndName>().apply {
                maxWidth = Double.MAX_VALUE
                items.addAll(TypeManager.allTypes.map { KeyAndName(it, getTypeTitle(it)) })
                selectionModel.select(0)
            }
            val nameField = TextField().apply {
                addEventHandler(KeyEvent.KEY_PRESSED) {
                    if (it.code == KeyCode.UP) {
                        typeCombo.selectPreviousOrLast()
                    }
                }
                addEventHandler(KeyEvent.KEY_PRESSED) {
                    if (it.code == KeyCode.DOWN) {
                        typeCombo.selectNextOrFirst()
                    }
                }
                textProperty().addListener { _, _, text ->
                    okButton.isDisable = text.isEmpty() || text in invalidKeys
                }
            }
            dialogPane.content = GridPane().apply {
                initAsForm(
                        listOf(
                                Label(tr("d.newVariant.name")), Label(tr("d.newVariant.type"))
                        ),
                        listOf(
                                nameField, typeCombo
                        )
                )
            }
            nameField.requestFocus()
            okButton.isDisable = true
            if (showAndWait().get() == ButtonType.OK) {
                val value = TypeManager.getDefault(typeCombo.selectionModel.selectedItem.key)
                variants += VariantItem(nameField.text.trim(), value?.also { newValues += it } ?: "")
                table.selectionModel.apply { clearSelection() }.selectLast()
                table.scrollTo(table.items.size - 1)
                isModified = true
            }
        }
    }

    private fun removeItem(items: Collection<VariantItem>, showWarn: Boolean = false) {
        if (items.isEmpty()) return
        if (showWarn) {
            val hint = if (items.size == 1) {
                tr("d.removeVariant.oneHint", getItemTitle(items.first().key))
            } else {
                tr("d.removeVariant.moreHint", items.size)
            }
            if (!confirm(tr("d.removeVariant.title"), hint)) {
                return
            }
        }
        for (item in items.toList()) {
            this.variants.remove(item)
            isModified = true
        }
    }

    private fun exportItem(item: VariantItem) {
        val value = item.data
        if (value is Flob) {
            val title = tr("d.exportItem.flob.title", item.key)
            selectSaveFile(title, value.name, scene.window)?.let {
                saveFlob(title, value, it, scene.window)
            }
        } else if (value is Text) {
            val title = tr("d.exportItem.text.title", item.key)
            selectSaveFile(title, "${item.key}.txt", scene.window)?.let {
                saveText(title, value, it, scene.window)
            }
        }
    }

    private inner class EditableDataCell<S, T : Any> : TableCell<S, T>() {
        private var comp: Node? = null

        override fun updateItem(item: T?, empty: Boolean) {
            super.updateItem(item, empty)
            if (empty) {
                text = null
                graphic = null
            } else {
                if (isEditing) {
                    text = null
                    graphic = comp
                } else {
                    text = item?.toString() ?: ""
                    graphic = null
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun startEdit() {
            if (isEmpty) return
            super.startEdit()

            val variant = selectedItem
            when (variant.type) {
                "" -> cancelEdit()
                TypeManager.BOOLEAN -> {
                    variant.data = !(variant.data as Boolean)
                    isModified = true
                    cancelEdit()
                }
                TypeManager.DATE -> {
                    graphic = DatePicker(variant.data as LocalDate).apply {
                        isShowWeekNumbers = true
                        setOnAction { commitEdit(value as T) }
                    }
                }
                TypeManager.LOCALE -> {
                    graphic = LocalePicker(variant.data as Locale).apply {
                        setOnAction { commitEdit(value as T) }
                    }
                }
                TypeManager.TEXT -> {
                    text(tr("d.editVariant.title", getItemTitle(variant.key)), variant.data.toString(), owner = scene.window)?.let {
                        variant.data = textOf(it)
                        isModified = true
                    }
                    cancelEdit()
                }
                TypeManager.FLOB -> {
                    selectOpenFile(tr("d.selectFile.title"), scene.window)?.let {
                        variant.data = flobOf(it.toPath())
                        isModified = true
                    }
                    cancelEdit()
                }
                else -> {
                    graphic = getAvailableValues(variant.key).takeIf { it.isNotEmpty() }?.let {
                        ComboBox<String>().apply {
                            items.setAll(it)
                            isEditable = true
                            editor.text = item.toString()
                            setOnAction { commitEdit(selectionModel.selectedItem as T) }
                        }
                    } ?: TextField(formatItemData(variant.data)).apply {
                        setOnAction { commitEdit(text as T) }
                    }
                }
            }
            graphic?.also {
                comp = it
                text = null
                (it as? Region)?.maxWidth = Double.MAX_VALUE
                it.requestFocus()
            }
        }

        override fun commitEdit(value: T) {
            val variant = selectedItem
            variant.data = if (value is String) {
                try {
                    TypeManager.parse(variant.type, value) ?: return
                } catch (e: IllegalArgumentException) {
                    return
                }
            } else {
                value
            }
            isModified = true

            super.commitEdit(value)
            tableView.requestFocus()
        }

        override fun cancelEdit() {
            super.cancelEdit()
            text = item?.toString() ?: ""
            graphic = null
        }
    }
}
