/*
 * Copyright 2015-2017 Peng Wan <phylame@163.com>
 *
 * This file is part of Jem.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
