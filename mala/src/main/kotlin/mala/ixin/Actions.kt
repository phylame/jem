package mala.ixin

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

typealias ActionMap = MutableMap<String, Action>
private const val COMMAND_KEY = "ixin-command-id"

class Action(val id: String) {
    val textProperty = SimpleStringProperty()
    val iconProperty = SimpleObjectProperty<Image>()
    val largeIconProperty = SimpleObjectProperty<Image>()
    val toastProperty = SimpleStringProperty()
    val acceleratorProperty = SimpleObjectProperty<KeyCombination>()
    val disableProperty = SimpleBooleanProperty()
    val selectedProperty = SimpleBooleanProperty()

    var text by textProperty
    var icon by iconProperty
    var largeIcon by largeIconProperty
    var toast by toastProperty
    var accelerator by acceleratorProperty
    var isDisable by disableProperty
    var isSelected by selectedProperty
}

fun ActionMap?.getOrCreate(id: String, m: Translator, r: AssetManager): Action {
    return this?.get(id) ?: loadAction(id, m, r).also { this?.set(id, it) }
}

fun Action.toButton(handler: CommandHandler, type: Style = Style.NORMAL, hideText: Boolean = false): ButtonBase {
    val button = when (type) {
        Style.NORMAL -> Button()
        Style.TOGGLE -> ToggleButton().also { it.selectedProperty().bindBidirectional(selectedProperty) }
        Style.RADIO -> RadioButton().also { it.selectedProperty().bindBidirectional(selectedProperty) }
        Style.CHECK -> CheckBox().also { it.selectedProperty().bindBidirectional(selectedProperty) }
        Style.LINK -> Hyperlink()
    }
    if (!hideText || (largeIcon == null && icon == null)) {
        button.textProperty().bind(textProperty)
    }
    button.tooltipProperty().bind(toastProperty.lazyTooltip())
    button.isMnemonicParsing = IxIn.isMnemonicEnable
    button.graphicProperty().bind(largeIconProperty.coalesce(iconProperty).lazyImageView())
    button.disableProperty().bindBidirectional(disableProperty)
    button.onAction = EventCommand(handler)
    button.properties[COMMAND_KEY] = id
    return button
}

fun Action.toMenuItem(handler: CommandHandler, type: Style = Style.NORMAL): MenuItem {
    val item = when (type) {
        Style.NORMAL -> MenuItem()
        Style.RADIO -> RadioMenuItem().also { it.selectedProperty().bindBidirectional(selectedProperty) }
        Style.CHECK -> CheckMenuItem().also { it.selectedProperty().bindBidirectional(selectedProperty) }
        else -> throw IllegalArgumentException("$type is not supported for menu item")
    }
    item.textProperty().bind(textProperty)
    item.graphicProperty().bind(iconProperty.lazyImageView())
    item.isMnemonicParsing = IxIn.isMnemonicEnable
    item.acceleratorProperty().bind(acceleratorProperty)
    item.disableProperty().bindBidirectional(disableProperty)
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
