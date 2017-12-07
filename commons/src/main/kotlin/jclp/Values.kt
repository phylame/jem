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

package jclp

import jclp.io.Flob
import jclp.io.emptyFlob
import jclp.io.getProperties
import jclp.log.Log
import jclp.text.ConverterManager
import jclp.text.Text
import jclp.text.emptyText
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.Temporal
import java.util.*
import java.util.function.Supplier

val <T : Any> Class<T>.objectType
    get() = if (isPrimitive) kotlin.javaObjectType else this

fun Class<*>.actualInstance(): Any = try {
    getField("INSTANCE").get(null) // for kotlin object
} catch (ignored: ReflectiveOperationException) {
    newInstance()
}

val Any?.actualValue
    get() = when (this) {
        is Function0<*> -> invoke()
        is Supplier<*> -> get()
        is Lazy<*> -> value
        else -> this
    }

object TypeManager {
    // standard type ids
    const val REAL = "real"
    const val INTEGER = "int"
    const val BOOLEAN = "bool"
    const val STRING = "str"
    const val LOCALE = "locale"
    const val DATE = "date"
    const val TIME = "time"
    const val DATETIME = "datetime"
    const val LEGACY_DATE = "jdate"
    const val TEXT = "text"
    const val FLOB = "file"

    private val types = hashMapOf<String, Item>()
    private val cache = hashMapOf<String, Item>()
    private val classes = IdentityHashMap<Class<*>, String>()

    init {
        initBuiltins()
        initDefaults()
    }

    val allTypes get() = types.keys + types.values.flatMap { it.aliases }

    fun setAlias(name: String, vararg aliases: String) {
        requiredType(name).aliases += aliases
    }

    fun mapClass(name: String, clazz: Class<*>) {
        require(name.isNotEmpty()) { "type name cannot be empty" }
        val objectClass = clazz.objectType
        getOrCreateType(name).clazz = objectClass
        classes.put(objectClass, name)
        cache.remove(name)
    }

    fun getClass(name: String) =
            if (name.isNotEmpty()) lookupType(name)?.clazz else null

    fun getType(value: Any) = getType(value.javaClass)

    fun getType(clazz: Class<*>) = classes.getOrPut(clazz.objectType) { cls ->
        types.entries.run {
            firstOrNull { it.value.clazz == cls } ?: firstOrNull { it.value.clazz!!.isAssignableFrom(cls) }
        }?.key
    }

    fun getName(name: String) =
            if (name.isNotEmpty()) M.optTr("type.$name") else null

    fun setDefault(name: String, value: Any) {
        requiredType(name).default = value
    }

    fun getDefault(name: String) =
            if (name.isNotEmpty()) lookupType(name)?.default?.actualValue else null

    fun parse(name: String, text: String) =
            getClass(name)?.let { type -> ConverterManager.parse(text, type) }

    fun printable(value: Any) = when (value) {
        is Locale -> value.displayName
        is Boolean -> M.tr("value.$value")
        is Date -> value.format(LOOSE_DATE_TIME_FORMAT)
        is Number, is CharSequence, is Temporal, is Text, is Flob -> value.toString()
        else -> null
    }

    private fun requiredType(name: String): Item {
        require(name.isNotEmpty()) { "type name cannot be empty" }
        return lookupType(name) ?: throw IllegalStateException("no such type for '$name'")
    }

    private fun getOrCreateType(name: String) =
            lookupType(name) ?: Item().apply { types[name] = this }

    private fun lookupType(name: String): Item? {
        var item = cache[name] ?: types[name]
        if (item != null) return item
        item = types.values.firstOrNull { name in it.aliases } ?: return null
        cache[name] = item
        return item
    }

    private fun initBuiltins() {
        getProperties("!jclp/types.properties")?.forEach {
            try {
                mapClass(it.value.toString(), Class.forName(it.key.toString()))
            } catch (e: ClassNotFoundException) {
                Log.e(javaClass.simpleName, e) { "cannot load type class" }
            }
        } ?: Log.w(javaClass.simpleName) { "not found type file: '!jclp/types.properties'" }
    }

    private fun initDefaults() {
        setDefault(REAL, 0.0)
        setDefault(INTEGER, 0)
        setDefault(STRING, "")
        setDefault(BOOLEAN, false)
        setDefault(DATE, { LocalDate.now() })
        setDefault(TIME, { LocalTime.now() })
        setDefault(DATETIME, { LocalDateTime.now() })
        setDefault(LEGACY_DATE, { Date() })
        setDefault(LOCALE, { Locale.getDefault() })
        setDefault(TEXT, emptyText())
        setDefault(FLOB, emptyFlob())
    }

    private class Item {
        var clazz: Class<*>? = null

        val aliases = hashSetOf<String>()

        var default: Any? = null
    }
}

typealias ValueValidator = (String, Any) -> Unit

class ValueMap(private val validator: ValueValidator? = null) : Iterable<VariantEntry>, Cloneable {
    private var map = hashMapOf<String, Any>()

    val size get() = map.size

    val names get() = map.keys

    fun isEmpty() = map.isEmpty()

    fun isNotEmpty() = map.isNotEmpty()

    operator fun set(name: String, value: Any): Any? {
        require(name.isNotEmpty()) { "name cannot be empty" }
        validator?.invoke(name, value)
        val old = map.put(name, value)
        value.retain()
        old.release()
        return old
    }

    fun update(map: VariantMap) {
        map.forEach { set(it.key, it.value) }
    }

    fun update(other: ValueMap) {
        update(other.map)
    }

    operator fun contains(name: String) = name.isNotEmpty() && name in map

    operator fun get(name: String) = if (name.isEmpty()) null else map[name]

    fun remove(name: String) = if (name.isEmpty()) null else map.remove(name)?.release()

    fun clear() {
        map.onEach { it.value.release() }.clear()
    }

    override fun iterator(): Iterator<VariantEntry> =
            map.entries.iterator()

    override fun toString() = map.toString()

    @Suppress("UNCHECKED_CAST")
    public override fun clone(): ValueMap {
        val copy = super.clone() as ValueMap
        copy.map = map.clone() as HashMap<String, Any>
        copy.map.onEach { it.value.retain() }
        return copy
    }
}
