package mala.ixin

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.Separator
import javafx.scene.input.KeyCombination
import jclp.Translator
import mala.AssetManager
import kotlin.reflect.KProperty

private const val COMMAND_KEY = "ixin-command-id"

operator fun <T> WritableValue<T>.getValue(ref: Any, property: KProperty<*>): T? {
    return value
}

operator fun <T> WritableValue<T>.setValue(ref: Any, property: KProperty<*>, value: T?) {
    this.value = value
}

enum class ButtonType {
    NORMAL, TOGGLE, RADIO, CHECK, LINK
}

class Action(val command: String) {
    val textProperty = SimpleStringProperty()
    val iconProperty = SimpleObjectProperty<Node>()
    val largeIconProperty = SimpleObjectProperty<Node>()
    val toastProperty = SimpleStringProperty()
    val descriptionProperty = SimpleStringProperty()
    val acceleratorProperty = SimpleObjectProperty<KeyCombination>()
    val disableProperty = SimpleBooleanProperty()
    val selectedProperty = SimpleBooleanProperty()

    var text: String? by textProperty
    var icon: Node? by iconProperty
    var largeIcon: Node? by largeIconProperty
    var toast: String? by toastProperty
    var description: String? by descriptionProperty
    var accelerator: KeyCombination? by acceleratorProperty
    var isDisable by disableProperty
    var isSelected by selectedProperty
}

fun loadAction(id: String, translator: Translator, assets: AssetManager): Action {
    TODO()
}

fun Action.toButton(type: ButtonType = ButtonType.NORMAL): ButtonBase {
    val button = when (type) {
        ButtonType.NORMAL -> Button()
        ButtonType.TOGGLE -> ToggleButton().also { it.selectedProperty().bindBidirectional(selectedProperty) }
        ButtonType.RADIO -> RadioButton().also { it.selectedProperty().bindBidirectional(selectedProperty) }
        ButtonType.CHECK -> CheckBox().also { it.selectedProperty().bindBidirectional(selectedProperty) }
        ButtonType.LINK -> Hyperlink()
    }
    button.textProperty().bind(textProperty)
    button.graphicProperty().bind(largeIconProperty.coalesce(iconProperty))
    button.disableProperty().bindBidirectional(disableProperty)

    val tooltipProperty = object : SimpleObjectProperty<Tooltip>(), ChangeListener<String> {
        override fun changed(observable: ObservableValue<out String>?, oldValue: String?, newValue: String?) {
            if (oldValue != newValue) {
                if (newValue.isNullOrEmpty()) {
                    value = null // hide tooltip
                } else if (value == null) {
                    value = Tooltip().apply { textProperty().bind(toastProperty) }
                }
            }
        }
    }

    toastProperty.addListener(tooltipProperty)
    button.tooltipProperty().bind(tooltipProperty)
    button.properties[COMMAND_KEY] = command
    return button
}

fun Action.toMenuItem(type: ButtonType = ButtonType.NORMAL): MenuItem {
    val item = when (type) {
        ButtonType.NORMAL -> MenuItem()
        ButtonType.RADIO -> RadioMenuItem().also { it.selectedProperty().bindBidirectional(selectedProperty) }
        ButtonType.CHECK -> CheckMenuItem().also { it.selectedProperty().bindBidirectional(selectedProperty) }
        else -> throw IllegalArgumentException("$type is not supported for menu item")
    }
    item.textProperty().bind(textProperty)
    item.graphicProperty().bind(iconProperty)
    item.acceleratorProperty().bind(acceleratorProperty)
    item.disableProperty().bindBidirectional(disableProperty)
    item.properties[COMMAND_KEY] = command
    return item
}

fun Menu.init(items: Iterable<*>) {
    for (item in items) {
        this.items += when (item) {
            null -> SeparatorMenuItem()
            is Action -> item.toMenuItem()
            else -> throw IllegalArgumentException("Unknown item $item")
        }
    }
}

fun ToolBar.init(items: Iterable<*>) {
    for (item in items) {
        this.items += when (item) {
            null -> Separator()
            is Action -> item.toButton()
            is Node -> item
            else -> throw IllegalArgumentException("Unknown item $item")
        }
    }
}
