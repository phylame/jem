package mala.ixin

import javafx.scene.Node
import javafx.scene.control.Menu
import javafx.scene.control.Separator
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.control.ToolBar
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCombination
import jclp.Translator
import jclp.text.or
import mala.AssetManager
import org.json.JSONArray
import org.json.JSONObject

enum class Style {
    NORMAL, TOGGLE, RADIO, CHECK, LINK
}

open class Item(
        val id: String,
        val isEnable: Boolean = true,
        val isSelected: Boolean = false,
        val style: Style = Style.NORMAL
) {
    init {
        require(id.isNotEmpty()) { "id of item cannot be empty" }
    }

    override fun hashCode() = id.hashCode()

    override fun equals(other: Any?) = other != null && other is Item && other.id == id

    override fun toString() = "${javaClass.simpleName}:id='$id', isEnable=$isEnable, isSelected=$isSelected, style=$style"
}

object Separator : Item("__SEPARATOR__")

open class ItemGroup(id: String, val items: Collection<Item>) : Item(id) {
    override fun toString() = "${super.toString()}, items=$items"
}

fun parseItems(array: JSONArray, items: MutableCollection<Item>) {
    for (item in array) {
        when (item) {
            is String -> items += Item(item)
            is JSONObject -> {
                val id = item.getString("id")
                items += item.optJSONArray("items")?.let {
                    with(ArrayList<Item>(it.length())) {
                        parseItems(it, this)
                        ItemGroup(id, this)
                    }
                } ?: Item(id,
                        item.optBoolean("enable", true),
                        item.optBoolean("selected", false),
                        Style.valueOf(item.optString("style", Style.NORMAL.name).toUpperCase()))
            }
            JSONObject.NULL -> items += Separator
            else -> throw IllegalArgumentException("Unexpected style of item: ${item.javaClass}")
        }
    }
}

fun loadAction(id: String, translator: Translator, assets: AssetManager) = Action(id).apply {
    text = translator.optTr(id) ?: ""

    val iconId = translator.optTr("$id.icon") or { "actions/$id" }
    assets.imageFor(iconId)?.let {
        icon = ImageView(it)
    }
    assets.imageFor("$iconId@x")?.let {
        largeIcon = ImageView(it)
    }

    translator.optTr("$id.toast")?.takeIf { it.isNotEmpty() }?.let {
        toast = it
    }
    translator.optTr("$id.description")?.takeIf { it.isNotEmpty() }?.let {
        description = it
    }
    translator.optTr("$id.accelerator")?.takeIf { it.isNotEmpty() }?.let {
        accelerator = KeyCombination.valueOf(it)
    }
}

fun Item.toAction(translator: Translator, assets: AssetManager): Action {
    require(this !== Separator) { "Separator cannot be created as action" }
    require(this !is ItemGroup) { "ItemGroup cannot be created as action" }
    return loadAction(id, translator, assets).also {
        it.isDisable = !isEnable
        it.isSelected = isSelected
    }
}

fun ItemGroup.toMenu(handler: CommandHandler, translator: Translator, assets: AssetManager) = Menu().also {
    it.text = translator.tr(id)
    it.init(items, handler, translator, assets)
}

fun Menu.init(items: Iterable<*>, handler: CommandHandler, translator: Translator, assets: AssetManager) {
    for (item in items) {
        this.items += when (item) {
            null, Separator -> SeparatorMenuItem()
            is Action -> item.toMenuItem(handler)
            is ItemGroup -> item.toMenu(handler, translator, assets)
            is Item -> item.toAction(translator, assets).toMenuItem(handler, item.style)
            else -> throw IllegalArgumentException("Unknown item $item")
        }
    }
}

fun ToolBar.init(items: Iterable<*>, handler: CommandHandler, translator: Translator, assets: AssetManager) {
    for (item in items) {
        this.items += when (item) {
            null, Separator -> Separator()
            is Action -> item.toButton(handler).also { it.styleClass += "toolbar-item" }
            is Item -> item.toAction(translator, assets).toButton(handler, item.style, true).also {
                it.styleClass += "toolbar-item"
            }
            is Node -> item
            else -> throw IllegalArgumentException("Unknown item $item")
        }
    }
}
