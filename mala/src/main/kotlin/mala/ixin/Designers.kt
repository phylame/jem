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

fun loadAction(id: String, m: Translator, r: AssetManager) = Action(id).apply {
    text = m.optTr(id) ?: id
    val iconId = m.optTr("$id.icon") or { "actions/$id" }
    icon = r.imageFor(iconId)
    largeIcon = r.imageFor("$iconId${IxIn.largeIconSuffix}")
    selectedIcon = r.imageFor("$iconId${IxIn.selectedIconSuffix}")
    toast = m.optTr("$id.toast")
    m.optTr("$id.accelerator")?.takeIf { it.isNotEmpty() }?.let {
        accelerator = KeyCombination.valueOf(it)
    }
}

fun Item.toAction(m: Translator, r: AssetManager, am: ActionMap?): Action {
    require(this !is ItemGroup) { "ItemGroup cannot be created as action" }
    require(this !== Separator) { "Separator cannot be created as action" }
    return am?.get(id) ?: loadAction(id, m, r).also {
        am?.set(id, it)
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

fun ItemGroup.toMenu(handler: CommandHandler, m: Translator, r: AssetManager, am: ActionMap?, mm: MenuMap?): Menu {
    return mm?.get(id) ?: Menu().also {
        mm?.set(id, it)
        it.text = m.optTr(id) ?: id
        it.init(items, handler, m, r, am, mm)
    }
}

fun Menu.init(items: Iterable<*>, handler: CommandHandler, m: Translator, r: AssetManager, am: ActionMap?, mm: MenuMap?) {
    for (item in items) {
        this.items += when (item) {
            null, Separator -> SeparatorMenuItem()
            is Action -> item.toMenuItem(handler)
            is ItemGroup -> item.toMenu(handler, m, r, am, mm)
            is Item -> item.toAction(m, r, am).toMenuItem(handler, item.style)
            is String -> am.getOrCreate(item, m, r).toMenuItem(handler)
            is Node -> CustomMenuItem(item)
            else -> throw IllegalArgumentException("Unknown item $item")
        }
    }
}

fun ContextMenu.init(items: Iterable<*>, handler: CommandHandler, m: Translator, r: AssetManager, am: ActionMap?, mm: MenuMap?) {
    for (item in items) {
        this.items += when (item) {
            null, Separator -> SeparatorMenuItem()
            is Action -> item.toMenuItem(handler)
            is ItemGroup -> item.toMenu(handler, m, r, am, mm)
            is Item -> item.toAction(m, r, am).toMenuItem(handler, item.style)
            is String -> am.getOrCreate(item, m, r).toMenuItem(handler)
            is Node -> CustomMenuItem(item)
            else -> throw IllegalArgumentException("Unknown item $item")
        }
    }
}

fun ToolBar.init(items: Iterable<*>, handler: CommandHandler, m: Translator, r: AssetManager, am: ActionMap?) {
    for (item in items) {
        this.items += when (item) {
            null, Separator -> Separator()
            is Action -> item.toButton(handler, hideText = true)
            is Item -> item.toAction(m, r, am).toButton(handler, item.style, true)
            is String -> am.getOrCreate(item, m, r).toButton(handler, hideText = true)
            is Node -> item
            else -> throw IllegalArgumentException("Unknown item $item")
        }.also { it.isFocusTraversable = false }
    }
}

interface AppDesigner {
    val items: Map<String, Collection<Item>>
}

class JSONDesigner(stream: InputStream) : AppDesigner {
    override val items = hashMapOf<String, List<Item>>()

    init {
        val json = JSONObject(JSONTokener(stream))
        for (key in json.keySet()) {
            json.optJSONArray(key)?.let {
                items[key] = ArrayList<Item>(it.length()).apply { init(it) }
            }
        }
    }
}
