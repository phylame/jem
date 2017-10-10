package jem.imabw.plugin

import javafx.beans.binding.Bindings
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem
import jem.imabw.Imabw
import jem.imabw.toc.NavPane
import mala.App.optTr

class BatchRename : ImabwAddon {
    override val name: String get() = optTr("addon.batchRename.name") ?: "Batch Rename"

    override val description: String get() = optTr("addon.batchRename.desc") ?: "Batch rename chapter titles"

    override fun ready() {
        val menu = Imabw.menuMap["menuTools"] ?: return
        menu.items += SeparatorMenuItem()
        menu.items += MenuItem(name).also {
            it.disableProperty().bind(Bindings.isEmpty(NavPane.treeView.selectionModel.selectedItems))
            it.setOnAction { doRename() }
        }
    }

    fun doRename() {
        println(NavPane.treeView.selectionModel.selectedItems)
    }
}
