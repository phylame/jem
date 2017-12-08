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

package jclp.setting

import jclp.VariantMap
import jclp.actualValue
import jclp.log.Log
import jclp.objectType
import jclp.text.ConverterManager
import jclp.update
import java.io.Reader
import java.io.Writer
import java.util.*

private typealias Predication = (Any?) -> Boolean

data class Dependency(val key: String, val condition: Predication? = null)

data class Definition(
        val key: String,
        val type: Class<*>,
        var default: Any? = null,
        var description: String = "",
        var constraint: Predication? = null,
        var dependencies: List<Dependency> = emptyList()
)

abstract class AbstractSettings : Settings {
    private val definitions = hashMapOf<String, Definition>()

    protected abstract fun handleGet(key: String): Any?

    protected abstract fun handleSet(key: String, value: Any): Any?

    protected abstract fun <T : Any> convertValue(value: Any, type: Class<T>): T

    override fun isEnable(key: String): Boolean = definitions[key]?.dependencies?.all { dep ->
        isEnable(dep.key) && dep.condition?.invoke(get(dep.key)) != false
    } != false

    fun getDefinition(key: String) = definitions[key]

    fun setDefinition(key: String, definition: Definition) = definitions.put(key, definition)

    private fun getDefault(key: String) = definitions[key]?.default?.actualValue

    override fun get(key: String) = handleGet(key) ?: getDefault(key)

    @Suppress("UNCHECKED_CAST")
    override operator fun <T : Any> get(key: String, type: Class<T>): T? {
        val value = handleGet(key) ?: return getDefault(key) as? T
        val objectClass = type.objectType
        return if (objectClass.isInstance(value)) {
            value as T
        } else {
            convertValue(value, objectClass)
        }
    }

    override fun set(key: String, value: Any): Any? {
        definitions[key]?.let {
            if (!it.type.isInstance(value)) {
                throw IllegalArgumentException("'$key' require type of '${it.type}'")
            }
            if (it.constraint?.invoke(value) == false) {
                throw IllegalArgumentException("Illegal value '$value' for '$key'")
            }
        }
        return handleSet(key, value)
    }

    override fun contains(key: String) = handleGet(key) != null
}

open class MapSettings(map: VariantMap? = null, definitions: Map<String, Definition>? = null) : AbstractSettings() {
    private val values = hashMapOf<String, Any>()

    init {
        map?.let { values += it }
        definitions?.forEach { setDefinition(it.key, it.value) }
        initValues()
    }

    override fun handleGet(key: String) = values[key]

    override fun contains(key: String) = key in values

    override fun handleSet(key: String, value: Any) = values.put(key, value)

    override fun remove(key: String) = values.remove(key)

    override fun iterator() = values.iterator()

    override fun clear() = values.clear()

    override fun <T : Any> convertValue(value: Any, type: Class<T>) = (value as? String)?.let {
        ConverterManager.parse(it, type)
    } ?: throw IllegalStateException("value is not a string: $value")

    override fun toString() = values.toString()

    fun load(reader: Reader) {
        val props = Properties().apply { load(reader) }
        if (props.isNotEmpty()) {
            values.update(props)
            initValues()
        }
    }

    fun sync(writer: Writer, comment: String? = null) {
        val props = Properties()
        for ((key, value) in values) {
            props[key] = ConverterManager.render(value)
        }
        props.store(writer, comment)
    }

    private fun initValues() {
        for (entry in values) {
            val type = getDefinition(entry.key)?.type ?: continue
            try {
                entry.setValue(ConverterManager.parse(entry.value.toString(), type) ?: continue)
            } catch (e: Exception) {
                Log.e(javaClass.simpleName, e) { "invalid value(${entry.value}) for type($type)" }
            }
        }
    }
}

fun VariantMap.toSettings() = MapSettings(this)
