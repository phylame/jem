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

package mala

import jclp.io.createRecursively
import jclp.setting.MapSettings
import java.io.File

interface Describable {
    val name: String

    val version: String

    val description: String

    val vendor: String
}

open class AppSettings(name: String, load: Boolean = true, sync: Boolean = true) : MapSettings() {
    val file = File(App.home, name)

    init {
        if (load && file.exists()) {
            @Suppress("LeakingThis")
            file.reader().use(this::load)
        }
        if (sync) {
            App.registerCleanup({
                if (file.exists() || file.parentFile.createRecursively()) {
                    sync(file.writer())
                } else {
                    App.error("cannot create directory for file: $file")
                }
            })
        }
    }
}
