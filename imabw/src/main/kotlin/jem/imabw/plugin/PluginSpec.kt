package jem.imabw.plugin

import jem.Build
import mala.Describable
import mala.Plugin

interface ImabwAddon : Plugin, Describable {
    override val version get() = Build.VERSION

    override val vendor get() = Build.VENDOR

    fun ready() {}
}
