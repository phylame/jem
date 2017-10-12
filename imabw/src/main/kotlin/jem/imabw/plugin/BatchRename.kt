package jem.imabw.plugin

import javafx.beans.binding.Bindings
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem
import jem.Build
import jem.imabw.toc.NavPane
import mala.App.optTr
import mala.Describable
import mala.ixin.IPlugin
import mala.ixin.IxIn

class BatchRename : IPlugin, Describable {
    override val version = Build.VERSION

    override val vendor = Build.VENDOR

    override val name: String get() = optTr("addon.batchRename.name") ?: "Batch Rename"

    override val description: String get() = optTr("addon.batchRename.desc") ?: "Batch rename chapter titles"

    override fun ready() {
        val menu = IxIn.menuMap["menuTools"] ?: return
        menu.items += SeparatorMenuItem()
        menu.items += MenuItem(name).also {
            it.disableProperty().bind(Bindings.isEmpty(NavPane.selection))
            it.setOnAction { doRename() }
        }
    }

    fun doRename() {
        println(NavPane.selection)
    }
}
