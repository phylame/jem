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

package jclp.setting

import jclp.actualValue
import jclp.canonicalType
import jclp.log.Log
import jclp.putAll
import jclp.text.Converters
import jclp.text.or
import java.io.Reader
import java.io.Writer
import java.util.*
import kotlin.reflect.KProperty

private typealias Predicate = (Any?) -> Boolean

interface Settings : Iterable<Pair<String, Any>> {
    fun isEnable(key: String): Boolean

    operator fun get(key: String): Any?

    fun <T : Any> get(key: String, type: Class<T>): T?

    operator fun set(key: String, value: Any): Any?

    operator fun contains(key: String): Boolean

    fun update(values: Map<String, Any>) {
        for ((key, value) in values) {
            set(key, value)
        }
    }

    fun update(settings: Settings) {
        for ((first, second) in settings) {
            set(first, second)
        }
    }

    fun remove(key: String): Any?

    fun clear()
}

fun Settings.getInt(key: String) = get(key, Int::class.java)

fun Settings.getDouble(key: String) = get(key, Double::class.java)

fun Settings.getString(key: String) = get(key, String::class.java)

fun Settings.getBoolean(key: String) = get(key, Boolean::class.java)

operator fun Settings.plusAssign(values: Map<String, Any>) {
    update(values)
}

operator fun Settings.plusAssign(settings: Settings) {
    update(settings)
}

class SettingsDelegate<T : Any>(private val type: Class<T>, private val default: T, private val key: String = "") {
    @Suppress("UNCHECKED_CAST")
    operator fun getValue(settings: Settings, property: KProperty<*>): T {
        return settings.get(key or { property.name }, type) ?: default
    }

    operator fun setValue(settings: Settings, property: KProperty<*>, value: T) {
        settings[key or { property.name }] = value
    }
}

inline fun <reified T : Any> Settings.delegate(default: T, key: String = ""): SettingsDelegate<T> {
    return SettingsDelegate(T::class.java, default, key)
}

abstract class AbstractSettings : Settings {
    private val definitions = HashMap<String, Definition>()

    protected abstract fun handleGet(key: String): Any?

    protected abstract fun handleSet(key: String, value: Any): Any?

    protected abstract fun <T : Any> convertValue(value: Any, type: Class<T>): T?

    override fun isEnable(key: String): Boolean = definitions[key]?.dependencies?.all {
        isEnable(it.key) && it.condition?.invoke(get(it.key)) != false
    } != false

    fun getDefinition(key: String) = definitions[key]

    fun setDefinition(key: String, definition: Definition) = definitions.put(key, definition)

    override fun get(key: String) = handleGet(key) ?: definitions[key]?.default?.actualValue

    @Suppress("UNCHECKED_CAST")
    override operator fun <T : Any> get(key: String, type: Class<T>): T? {
        val value = handleGet(key)
        val clazz = type.canonicalType
        return when {
            value == null -> definitions[key]?.default?.actualValue as? T
            clazz.isInstance(value) -> value as T
            else -> convertValue(value, clazz)
        }
    }

    override fun set(key: String, value: Any): Any? {
        definitions[key]?.let {
            it.type?.let {
                if (!it.isInstance(value)) {
                    throw IllegalArgumentException("'$key' require '$it'")
                }
            }
            if (it.constraint?.invoke(value) == false) {
                throw IllegalArgumentException("illegal '$value' for '$key'")
            }
        }
        return handleSet(key, value)
    }

    override fun contains(key: String) = handleGet(key) != null
}

open class MapSettings(values: Map<*, Any>? = null, definitions: Map<String, Definition>? = null) : AbstractSettings() {
    private val values = HashMap<String, Any>()

    init {
        values?.entries?.forEach {
            set(it.key.toString(), it.value)
        }
        definitions?.entries?.forEach {
            setDefinition(it.key, it.value)
        }
        initValues()
    }

    override fun handleGet(key: String) = values[key]

    override fun handleSet(key: String, value: Any) = values.put(key, value)

    override fun iterator() = values.entries.map { it.toPair() }.iterator()

    override fun remove(key: String) = values.remove(key)

    override fun clear() = values.clear()

    override fun <T : Any> convertValue(value: Any, type: Class<T>) = (value as? String)?.let {
        Converters.parse(it, type)
    } ?: throw IllegalStateException("value is not string $value")

    override fun toString() = values.toString()

    fun load(reader: Reader) {
        val props = Properties()
        props.load(reader)
        if (props.isNotEmpty()) {
            values.putAll(props)
            initValues()
        }
    }

    fun sync(writer: Writer, comment: String? = null) {
        val props = Properties()
        for ((key, value) in values) {
            props[key] = value as? CharSequence ?: Converters.render(value, value.javaClass)
        }
        props.store(writer, comment)
    }

    private fun initValues() {
        for (entry in values) {
            val type = getDefinition(entry.key)?.type ?: continue
            try {
                entry.setValue(Converters.parse(entry.value.toString(), type) ?: continue)
            } catch (e: Exception) {
                Log.e(javaClass.simpleName, e) { "invalid value(${entry.value}) for type($type)" }
            }
        }
    }
}

data class Dependency(val key: String, val condition: Predicate? = null)

data class Definition(
        val key: String,
        var type: Class<*>? = null,
        var default: Any? = null,
        var description: String = "",
        var constraint: Predicate? = null,
        var dependencies: List<Dependency> = emptyList()
)
