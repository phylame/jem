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
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.Region
import jclp.*
import jclp.io.Flob
import jclp.io.flobOf
import jclp.text.*
import jem.Attributes
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

internal class VariantItem(key: String, data: Any) {
    val keyProperty: SimpleStringProperty = SimpleStringProperty(key)

    val dataProperty: SimpleObjectProperty<Any> = SimpleObjectProperty(data)

    val typeProperty: StringBinding = Bindings.createStringBinding(Callable {
        TypeManager.getType(dataProperty.value) ?: ""
    }, dataProperty)

    val key: String inline get() = keyProperty.value

    val type: String inline get() = typeProperty.value

    var data: Any
        inline get() = dataProperty.value
        inline set(value) {
            dataProperty.value = value
        }
}

open class VariantPane(private val values: ValueMap, private val tagId: String, showName: Boolean = true) : BorderPane() {
    private val valueRef = LinkedList<Any>()

    private val variants = FXCollections.observableArrayList<VariantItem>()

    private val currentKeys inline get() = variants.map { it.key }

    private val table = TableView<VariantItem>(variants)

    private val toolbar = ToolBar()

    var isModified = false
        private set

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
        for (name in values.names.toList() - ignoredKeys()) {
            values.remove(name)
        }
        for (item in variants) {
            item.key.ifNotEmpty { values[it] = item.data }
        }
        variants.forEach { it.data.release() }
        valueRef.forEach { it.release() }
    }

    fun storeState() {
        UISettings.storeState(table, tagId)
    }

    protected open fun getItemTitle(key: String): String = key

    protected open fun getTypeTitle(type: String): String = TypeManager.getTitle(type) or type

    protected open fun formatItemData(value: Any): String = TypeManager.printable(value) or { value.toString() }

    protected open fun getItemType(key: String): String? = null

    protected open fun getDefaultValue(key: String): Any = ""

    protected open fun isMultiValues(key: String): Boolean = false

    protected open fun getAvailableValues(key: String): List<String> = emptyList()

    protected open fun availableKeys(): Collection<String> = emptyList()

    protected open fun ignoredKeys(): Collection<String> = emptyList()

    protected open fun dialogNewTitle() = tr("d.newVariant.title")

    private fun initTable(showTitle: Boolean) {
        with(table) {
            isEditable = true
            selectionModel.selectionMode = SelectionMode.MULTIPLE
            addEventHandler(KeyEvent.KEY_PRESSED) { event ->
                if (event.code == KeyCode.DELETE) {
                    removeItem(table.selectionModel.selectedItems, true)
                }
            }
            addEventHandler(KeyEvent.KEY_PRESSED) { event ->
                if (event.code == KeyCode.INSERT) {
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
            columns += TableColumn<VariantItem, Any>(tr("com.variant.value")).apply {
                setOnEditCommit { event ->
                    if (event.newValue != event.oldValue) {
                        event.rowValue.data = event.newValue
                        isModified = true
                    }
                }
                setCellFactory { EditableDataCell() }
                setCellValueFactory { it.value.dataProperty }
            }
            UISettings.restoreState(this, tagId)
        }
    }

    private fun initToolBar() {
        val selection = table.selectionModel.selectedItems
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
                setOnAction { exportItem(table.selectionModel.selectedItem) }
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
        values.filter { it.key !in ignoredKeys() }.mapTo(variants) { VariantItem(it.key, it.value) }
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
                    variants += VariantItem(item.key, getDefaultValue(item.key).also { valueRef += it })
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
                variants += VariantItem(nameField.text.trim(), value?.also { valueRef += it } ?: "")
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
            variants.remove(item)
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

    private inner class EditableDataCell : TableCell<VariantItem, Any>() {
        private var editor: Node? = null

        override fun updateItem(item: Any?, empty: Boolean) {
            super.updateItem(item, empty)
            when {
                empty -> {
                    text = null
                    graphic = null
                }
                isEditing -> {
                    text = null
                    graphic = editor?.apply { updateEditor(item!!) }
                }
                else -> {
                    text = formatItemData(item!!)
                    graphic = null
                }
            }
        }

        override fun startEdit() {
            if (isEmpty) return
            super.startEdit()
            createEditor(item)
            editor?.let {
                text = null
                graphic = it
                (it as? Region)?.maxWidth = Double.MAX_VALUE
                it.requestFocus()
            }
        }

        override fun cancelEdit() {
            super.cancelEdit()
            text = formatItemData(item)
            graphic = null
        }

        private fun createEditor(data: Any) {
            editor = null
            val key = variants[index].key
            when (data) {
                is CharSequence -> {
                    if (isMultiValues(key)) {
                        val str = data.toString().replace(Attributes.VALUE_SEPARATOR, "\n")
                        text(tr("d.editVariant.title", getItemTitle(key)), initial = str, mustDiff = true, owner = scene.window)?.let {
                            commitEdit(it.split("\n").filter { it.isNotEmpty() }.joinToString(Attributes.VALUE_SEPARATOR))
                        }
                        cancelEdit()
                        return
                    }
                    editor = getAvailableValues(key).ifNotEmpty { values ->
                        ComboBox<String>().apply {
                            isEditable = true
                            items.setAll(values)
                            editor.text = data.toString()
                            setOnAction { commitEdit(selectionModel.selectedItem) }
                        }
                    } ?: TextField(data.toString()).apply {
                        setOnAction { commitEdit(text) }
                        focusedProperty().addListener { _, _, focused ->
                            if (!focused) commitEdit(text)
                        }
                    }
                }
                is LocalDate -> {
                    editor = DatePicker(data).apply {
                        isShowWeekNumbers = true
                        setOnAction { commitEdit(value) }
                        editor.focusedProperty().addListener { _, _, focused ->
                            if (!focused) commitEdit(converter.fromString(editor.text))
                        }
                    }
                }
                is Locale -> {
                    editor = LocalePicker(data).apply {
                        setOnAction { commitEdit(value) }
                    }
                }
                is Text -> {
                    text(tr("d.editVariant.title", getItemTitle(key)), data.toString(), owner = scene.window)?.let {
                        commitEdit(textOf(it))
                    }
                    cancelEdit()
                }
                is Flob -> {
                    selectOpenFile(tr("d.selectFile.title"), scene.window)?.let {
                        commitEdit(flobOf(it))
                    }
                    cancelEdit()
                }
                is Boolean -> {
                    commitEdit(!data)
                }
                else -> {
                    val type = data.javaClass
                    if (type in ConverterManager) {
                        editor = TextField(ConverterManager.render(data)).apply {
                            setOnAction {
                                try {
                                    commitEdit(ConverterManager.parse(text, type))
                                } catch (e: RuntimeException) {
                                    App.error("invalid value", e)
                                }
                            }
                            focusedProperty().addListener { _, _, focused ->
                                if (!focused) {
                                    try {
                                        commitEdit(ConverterManager.parse(text, type))
                                    } catch (e: RuntimeException) {
                                        App.error("invalid value", e)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        private fun updateEditor(data: Any) {
            println("update editor for $data")
            val comp = editor
            when (comp) {
                is TextArea -> {
                    comp.text = data.toString()
                }
                is DatePicker -> {
                    comp.value = data as LocalDate
                }
            }
        }
    }
}
