package mala.ixin

import jclp.Translator
import mala.AssetManager


open class Item(
        val id: String,
        val isEnable: Boolean = true,
        val isSelected: Boolean = false,
        val type: ButtonType = ButtonType.NORMAL
) {
    init {
        require(id.isNotEmpty()) { "id of item cannot be empty" }
    }

    override fun hashCode() = id.hashCode()

    override fun equals(other: Any?) = other != null && other is Item && other.id == id

    override fun toString() = "${javaClass.simpleName}(id='$id', isEnable=$isEnable, isSelected=$isSelected, type=$type)"
}

object Separator : Item("__SEPARATOR__")

open class Group(id: String, val items: Array<Item>) : Item(id)

fun Item.toAction(translator: Translator, assets: AssetManager): Action {
    require(this !== Separator) { "Separator cannot be created as action" }
    require(this !is Group) { "Group cannot be created as action" }
    return loadAction(id, translator, assets).also {
        it.isDisable = !isEnable
        it.isSelected = isSelected
    }
}
