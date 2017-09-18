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

package jem.scj.addon

import jclp.Linguist
import jclp.attach
import jem.Build
import mala.App
import mala.Describable
import mala.Plugin

private var isAttached = false

internal fun attachTranslator() {
    if (!isAttached) {
        App.attach(Linguist("!jem/scj/addon/messages"))
        isAttached = true
    }
}

abstract class SCJAddon : Plugin, Describable {
    companion object {
        init {
            attachTranslator()
        }
    }

    override val version = Build.VERSION

    override val vendor = Build.VENDOR
}
