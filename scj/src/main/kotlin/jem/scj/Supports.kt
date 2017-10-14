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

package jem.scj

import jclp.setting.delegate
import jem.COVER
import jem.TITLE
import jem.epm.PMAB_NAME
import mala.AppSettings

object SCISettings : AppSettings() {
    var termWidth by delegate(80, "app.termWidth")

    var outputFormat by delegate(PMAB_NAME, "jem.out.format")

    var separator by delegate("\n", "sci.view.separator")

    var skipEmpty by delegate(true, "sci.view.skipEmpty")

    var tocIndent by delegate("  ", "sci.view.tocIndent")

    var tocNames
        inline get() = (get("sci.view.tocNames") as? String)?.split(",") ?: listOf(TITLE, COVER)
        inline set(value) {
            set("sci.view.tocNames", value.joinToString(","))
        }

    fun viewSettings() = ViewSettings(separator, skipEmpty, tocIndent, tocNames)
}
