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

package mala

import jclp.Linguist
import jclp.io.loadProperties
import jclp.io.getResource
import java.util.*

class AssetManager(base: String, private val loader: ClassLoader? = null) {
    private val home: String

    init {
        require(base.isNotEmpty()) { "base path cannot be empty" }
        val path = base.replaceFirst("$!/", "!")
        home = if (path != "!" && !path.endsWith("/")) path + '/' else path
    }

    fun pathOf(name: String) = home + name.trimStart('/')

    fun resourceFor(name: String) = getResource(pathOf(name), loader)

    fun propertiesFor(name: String, reload: Boolean = false) = loadProperties(pathOf(name), loader, reload)

    fun translatorFor(name: String, locale: Locale? = null, dummy: Boolean = true) = Linguist(pathOf(name), locale, loader, dummy)

    override fun toString() = "AssetManager(home='$home')"
}
