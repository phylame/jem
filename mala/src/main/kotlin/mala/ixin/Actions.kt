package mala.ixin

import javafx.beans.binding.ObjectBinding
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.input.KeyCombination
import jclp.Translator
import mala.AssetManager
import java.util.*

private const val COMMAND_KEY = "ixin-command-id"

class Action(val id: String) {
    val textProperty = SimpleStringProperty()
    val iconProperty = SimpleObjectProperty<Image>()
    val largeIconProperty = SimpleObjectProperty<Image>()
    val selectedIconProperty = SimpleObjectProperty<Image>()
    val toastProperty = SimpleStringProperty()
    val acceleratorProperty = SimpleObjectProperty<KeyCombination>()
    val disableProperty = SimpleBooleanProperty()
    val selectedProperty = SimpleBooleanProperty()

    var text by textProperty
    var icon by iconProperty
    var largeIcon by largeIconProperty
    var selectedIcon by selectedIconProperty
    var toast by toastProperty
    var accelerator by acceleratorProperty
    var isDisable by disableProperty
    var isSelected by selectedProperty

    init {
        acceleratorProperty.addListener { _, old, new ->
            if (old != new && old == null && !toast.isNullOrEmpty()) {
                toast = "$toast (${new.displayText})"
            }
        }
    }

    override fun toString(): String {
        return "Action(id='$id', text='$text', icon='$icon', toast='$toast', isDisable=$isDisable, isSelected=$isSelected)"
    }
}

fun ActionMap?.getOrCreate(id: String, m: Translator, r: AssetManager): Action {
    return this?.get(id) ?: loadAction(id, m, r).also { this?.set(id, it) }
}

fun ActionMap.updateAccelerators(keys: Map<String, *>) {
    for ((key, value) in keys) {
        get(key)?.accelerator = value as? KeyCombination ?: KeyCombination.valueOf(value.toString())
    }
}

fun ActionMap.updateAccelerators(keys: Properties) {
    for ((key, value) in keys) {
        get(key)?.accelerator = value as? KeyCombination ?: KeyCombination.valueOf(value.toString())
    }
}

val Action.actualIconBinding
    get() = object : ObjectBinding<Image>() {
        init {
            bind(iconProperty, selectedIconProperty, selectedProperty)
        }

        override fun computeValue() = if (!selectedProperty.value) icon else selectedIcon ?: icon
    }

val Action.largeActualIconBinding
    get() = object : ObjectBinding<Image>() {
        init {
            bind(iconProperty, largeIconProperty, selectedIconProperty, selectedProperty)
        }

        override fun computeValue() = if (!selectedProperty.value) largeIcon ?: icon else selectedIcon ?: largeIcon ?: icon
    }

fun Action.toButton(handler: CommandHandler, type: Style = Style.NORMAL, hideText: Boolean = false): ButtonBase {
    val button = when (type) {
        Style.NORMAL -> Button()
        Style.TOGGLE -> ToggleButton().apply { selectedProperty().bindBidirectional(selectedProperty) }
        Style.RADIO -> RadioButton().apply { selectedProperty().bindBidirectional(selectedProperty) }
        Style.CHECK -> CheckBox().apply { selectedProperty().bindBidirectional(selectedProperty) }
        Style.LINK -> Hyperlink()
    }
    if (!hideText || (largeIcon == null && icon == null)) {
        button.textProperty().bind(textProperty)
    }
    button.isMnemonicParsing = IxIn.isMnemonicEnable
    button.tooltipProperty().bind(toastProperty.lazyTooltip())
    button.graphicProperty().bind(largeActualIconBinding.lazyImageView())
    button.disableProperty().bindBidirectional(disableProperty)
    button.onAction = EventCommand(handler)
    button.properties[COMMAND_KEY] = id
    return button
}

fun Action.toMenuItem(handler: CommandHandler, type: Style = Style.NORMAL): MenuItem {
    val item = when (type) {
        Style.NORMAL -> MenuItem()
        Style.RADIO -> RadioMenuItem().apply { selectedProperty().bindBidirectional(selectedProperty) }
        Style.CHECK -> CheckMenuItem().apply { selectedProperty().bindBidirectional(selectedProperty) }
        else -> throw IllegalArgumentException("$type is not supported for menu item")
    }
    item.textProperty().bind(textProperty)
    item.isMnemonicParsing = IxIn.isMnemonicEnable
    item.graphicProperty().bind(actualIconBinding.lazyImageView())
    item.disableProperty().bindBidirectional(disableProperty)
    item.acceleratorProperty().bind(acceleratorProperty)
    item.onAction = EventCommand(handler)
    item.properties[COMMAND_KEY] = id
    return item
}

private class EventCommand(val handler: CommandHandler) : EventHandler<ActionEvent> {
    override fun handle(event: ActionEvent) {
        val source = event.source
        when (source) {
            is MenuItem -> handler.handle(source.properties[COMMAND_KEY]!!.toString(), source)
            is ButtonBase -> handler.handle(source.properties[COMMAND_KEY]!!.toString(), source)
            else -> throw IllegalStateException("Event source is not button or menu: $source")
        }
    }
}
