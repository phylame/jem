package mala.ixin

import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.Separator
import javafx.scene.input.KeyCombination
import jclp.Translator
import jclp.text.or
import mala.AssetManager
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.InputStream
import java.util.*

enum class Style {
    NORMAL, TOGGLE, RADIO, CHECK, LINK
}

open class Item(val id: String, val isEnable: Boolean = true, val isSelected: Boolean = false, val style: Style = Style.NORMAL) {
    init {
        require(id.isNotEmpty()) { "id of item cannot be empty" }
    }

    override fun hashCode() = id.hashCode()

    override fun equals(other: Any?) = other != null && other is Item && other.id == id

    override fun toString() = "${javaClass.simpleName}:id='$id', isEnable=$isEnable, isSelected=$isSelected, style=$style"
}

open class ItemGroup(id: String, val items: Collection<Item>) : Item(id) {
    override fun toString() = "${super.toString()}, items=$items"
}

object Separator : Item("__SEPARATOR__")

fun loadAction(id: String, translator: Translator, assets: AssetManager) = Action(id).apply {
    text = translator.optTr(id) ?: ""
    val iconId = translator.optTr("$id.icon") or { "actions/$id" }
    icon = assets.graphicFor(iconId)
    largeIcon = assets.graphicFor("$iconId@x")
    toast = translator.optTr("$id.toast")
    translator.optTr("$id.accelerator")?.takeIf { it.isNotEmpty() }?.let {
        accelerator = KeyCombination.valueOf(it)
    }
}

fun Item.toAction(translator: Translator, assets: AssetManager, actions: ActionMap?): Action {
    require(this !is ItemGroup) { "ItemGroup cannot be created as action" }
    require(this !== Separator) { "Separator cannot be created as action" }
    return actions?.getOrPut(id) {
        loadAction(id, translator, assets).also {
            it.isDisable = !isEnable
            it.isSelected = isSelected
        }
    } ?: loadAction(id, translator, assets).also {
        it.isDisable = !isEnable
        it.isSelected = isSelected
    }
}

fun MutableCollection<Item>.init(items: JSONArray) {
    for (item in items) {
        this += when (item) {
            is String -> Item(item)
            is JSONObject -> {
                item.optJSONArray("items")?.let {
                    with(ArrayList<Item>(it.length())) {
                        init(it)
                        ItemGroup(item.getString("id"), this)
                    }
                } ?: Item(item.getString("id"),
                        item.optBoolean("enable", true),
                        item.optBoolean("selected", false),
                        Style.valueOf(item.optString("style", Style.NORMAL.name).toUpperCase()))
            }
            JSONObject.NULL -> Separator
            else -> throw IllegalArgumentException("Unexpected style of item: ${item.javaClass}")
        }
    }
}

fun ItemGroup.toMenu(handler: CommandHandler, translator: Translator, assets: AssetManager, actions: ActionMap?) = Menu().also {
    it.text = translator.tr(id)
    it.init(items, handler, translator, assets, actions)
}

fun Menu.init(items: Iterable<*>, handler: CommandHandler, translator: Translator, assets: AssetManager, actions: ActionMap?) {
    for (item in items) {
        this.items += when (item) {
            null, Separator -> SeparatorMenuItem()
            is Action -> item.toMenuItem(handler)
            is ItemGroup -> item.toMenu(handler, translator, assets, actions)
            is Item -> item.toAction(translator, assets, actions).toMenuItem(handler, item.style)
            is Node -> CustomMenuItem(item)
            else -> throw IllegalArgumentException("Unknown item $item")
        }
    }
}

fun ToolBar.init(items: Iterable<*>, handler: CommandHandler, translator: Translator, assets: AssetManager, actions: ActionMap) {
    for (item in items) {
        this.items += when (item) {
            null, Separator -> Separator()
            is Action -> item.toButton(handler, hideText = true)
            is Item -> item.toAction(translator, assets, actions).toButton(handler, item.style, true)
            is Node -> item
            else -> throw IllegalArgumentException("Unknown item $item")
        }
    }
}

interface AppDesigner {
    val menuBar: Collection<Item>?

    val toolBar: Collection<*>?
}

class JSONDesigner(stream: InputStream) : AppDesigner {
    override val menuBar = arrayListOf<Item>()

    override val toolBar = arrayListOf<Item>()

    init {
        val json = JSONObject(JSONTokener(stream))
        json.optJSONArray("menubar")?.let {
            menuBar.init(it)
        }
        json.optJSONArray("toolbar")?.let {
            toolBar.init(it)
        }
    }
}
