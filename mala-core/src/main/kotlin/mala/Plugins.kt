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

import mala.App.error
import mala.App.optTr
import java.io.InputStream
import java.util.*

interface Plugin {
    fun init() {}

    fun destroy() {}
}

private const val COMMENT_LABEL = "#"

class PluginManager(private val path: String, private val loader: ClassLoader? = null) {
    var isEnable: Boolean = false

    var filter: ((Plugin) -> Boolean)? = null

    var blacklist: Collection<String> = emptySet()

    private val plugins = LinkedList<Plugin>()

    fun init() {
        if (isEnable) {
            parseRegistries()
            plugins.forEach(Plugin::init)
        }
    }

    inline fun <reified T : Plugin> with(action: T.() -> Unit) {
        if (isEnable) {
            get(T::class.java).forEach(action)
        }
    }

    fun destroy() {
        if (isEnable) {
            plugins.onEach(Plugin::destroy).clear()
        }
    }

    operator fun <T : Plugin> get(type: Class<T>): List<T> {
        return plugins.filter(type::isInstance).map(type::cast)
    }

    operator fun iterator() = plugins.iterator()

    private fun parseRegistries() {
        val loader = loader ?: App.javaClass.classLoader
        for (url in loader.getResources(path)) {
            url.openStream().use { parseRegistry(it, loader) }
        }
    }

    private fun parseRegistry(input: InputStream, loader: ClassLoader) {
        input.bufferedReader().useLines {
            it.map(String::trim)
                    .filter { it.isNotEmpty() && !it.startsWith(COMMENT_LABEL) && it !in blacklist }
                    .forEach { loadPlugin(it, loader) }
        }
    }

    private fun loadPlugin(path: String, loader: ClassLoader) {
        try {
            val clazz = loader.loadClass(path)
            if (!Plugin::class.java.isAssignableFrom(clazz)) {
                error(optTr("mala.err.badPlugin", "plugin must be sub-class of ''{0}'': {1}", Plugin::class.java.name, path))
            }
            val plugin: Plugin = try {
                clazz.getField("INSTANCE").get(null) as Plugin // for kotlin object
            } catch (ignored: ReflectiveOperationException) {
                clazz.newInstance() as Plugin
            }
            if (filter?.invoke(plugin) != false) {
                plugins += plugin
            }
        } catch (e: ReflectiveOperationException) {
            error(optTr("mala.err.loadPlugin", "cannot load plugin: {0}", path), e)
        }
    }
}
