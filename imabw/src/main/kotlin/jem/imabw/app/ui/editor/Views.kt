/*
 * Copyright 2017 Peng Wan <phylame@163.com>
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

package jem.imabw.app.ui.editor

import jem.imabw.app.CLOSE_ALL_TABS
import jem.imabw.app.Imabw
import jem.imabw.app.REPLACE
import jem.imabw.app.ui.Editable
import org.jdesktop.swingx.JXPanel
import qaf.ixin.Command

object TabbedEditor : JXPanel(), Editable {
    init {
        Imabw.addProxy(this)
    }

    override fun undo() {
    }

    override fun redo() {
    }

    override fun cut() {
    }

    override fun copy() {
    }

    override fun paste() {
    }

    override fun delete() {
    }

    override fun selectAll() {
    }

    override fun find() {
    }

    override fun findNext() {
    }

    override fun findPrevious() {

    }

    override fun goto() {

    }

    @Command(REPLACE)
    fun replace() {

    }

    @Command(CLOSE_ALL_TABS)
    fun closeAllTabs() {
        clearTabs(true)
    }

    fun clearTabs(caching: Boolean) {

    }
}
