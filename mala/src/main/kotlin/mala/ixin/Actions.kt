package mala.ixin

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.WritableValue
import javafx.collections.FXCollections
import javafx.scene.Node
import javafx.scene.control.*
import kotlin.reflect.KProperty

private const val COMMAND_KEY = "ixin-command-id"

operator fun <T> WritableValue<T>.getValue(ref: Any, property: KProperty<*>): T {
    return value
}

operator fun <T> WritableValue<T>.setValue(ref: Any, property: KProperty<*>, value: T) {
    this.value = value
}

enum class ButtonType {
    NORMAL, TOGGLE, RADIO, CHECK, LINK
}

class Action(val command: String, val type: ButtonType = ButtonType.NORMAL) {
    private val textProperty = SimpleStringProperty()
    private val toastProperty = SimpleStringProperty()
    private val iconProperty = SimpleObjectProperty<Node>()
    private val disableProperty = SimpleBooleanProperty()
    private val selectedProperty = SimpleBooleanProperty()

    var text by textProperty
    var icon by iconProperty
    var toast by toastProperty
    var isDisable by disableProperty
    var isSelected by selectedProperty

    fun toButton(type: ButtonType? = null): ButtonBase {
        val button = when (type ?: this.type) {
            ButtonType.NORMAL -> Button()
            ButtonType.TOGGLE -> ToggleButton().also {
                it.selectedProperty().bindBidirectional(selectedProperty)
            }
            ButtonType.RADIO -> RadioButton().also {
                it.selectedProperty().bindBidirectional(selectedProperty)
            }
            ButtonType.CHECK -> CheckBox().also {
                it.selectedProperty().bindBidirectional(selectedProperty)
            }
            ButtonType.LINK -> Hyperlink()
        }
        button.textProperty().bind(textProperty)
        button.graphicProperty().bind(iconProperty)
        button.disableProperty().bindBidirectional(disableProperty)
        if (toast.isNotEmpty()) {
            button.tooltip = Tooltip().apply {
                textProperty().bind(toastProperty)
            }
        }
        button.properties[COMMAND_KEY] = command
        return button
    }

    fun toMenuItem(type: ButtonType? = null): MenuItem {
        val item = when (type ?: this.type) {
            ButtonType.NORMAL -> MenuItem()
            ButtonType.RADIO -> RadioMenuItem().also {
                it.selectedProperty().bindBidirectional(selectedProperty)
            }
            ButtonType.CHECK -> CheckMenuItem().also {
                it.selectedProperty().bindBidirectional(selectedProperty)
            }
            else -> throw IllegalArgumentException("$type is not supported for menu item")
        }
        item.textProperty().bind(textProperty)
        item.graphicProperty().bind(iconProperty)
        item.disableProperty().bindBidirectional(disableProperty)
        item.properties[COMMAND_KEY] = command
        return item
    }
}

class Group {
    private val textProperty = SimpleStringProperty()
    private val disableProperty = SimpleBooleanProperty()

    var text by textProperty
    var isDisable by disableProperty
    val items = FXCollections.observableArrayList<Any?>()

    fun toMenu(): Menu {
        val menu = Menu()
        menu.textProperty().bind(textProperty)
        menu.disableProperty().bind(disableProperty)
        for (item in items) {
            when (item) {
                null -> Unit
            }
        }
        return menu
    }
}
