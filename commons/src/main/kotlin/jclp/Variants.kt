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

package jclp

import jclp.flob.emptyFlob
import jclp.io.getProperties
import jclp.log.Log
import jclp.text.emptyText
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import java.util.function.Supplier

val Any?.actualValue
    get() = when (this) {
        is Function0<*> -> invoke()
        is Supplier<*> -> get()
        is Lazy<*> -> value
        else -> this
    }

val <T : Any> Class<T>.canonicalType
    get() = if (isPrimitive) kotlin.javaObjectType else this

fun Class<*>.createInstance(): Any = try {
    @Suppress("UNCHECKED_CAST")
    getField("INSTANCE").get(null) // for kotlin object
} catch (ignored: ReflectiveOperationException) {
    newInstance()
}

object Variants {
    // standard type ids
    const val REAL = "real"
    const val INTEGER = "int"
    const val BOOLEAN = "bool"
    const val STRING = "str"
    const val LOCALE = "locale"
    const val DATE = "date"
    const val TIME = "time"
    const val DATETIME = "datetime"
    const val TEXT = "text"
    const val FLOB = "file"

    private val types = hashMapOf<String, Item>()
    private val cache = hashMapOf<String, Item>()
    private val classes = IdentityHashMap<Class<*>, String>()

    init {
        initBuiltins()
        initDefaults()
    }

    val allTypes get() = types.keys

    fun setAlias(id: String, vararg aliases: String) {
        requiredType(id).aliases += aliases
    }

    fun mapClass(id: String, clazz: Class<*>) {
        require(id.isNotEmpty()) { "type id cannot be empty" }
        val type = clazz.canonicalType
        fetchType(id).clazz = type
        classes.put(type, id)
        cache.remove(id)
    }

    fun getClass(id: String) = if (id.isNotEmpty()) lookupType(id)?.clazz else null

    fun getType(obj: Any) = getType(obj.javaClass)

    fun getType(clazz: Class<*>) = classes.getOrPut(clazz.canonicalType) { cls ->
        types.entries.run {
            firstOrNull { it.value.clazz == cls } ?: firstOrNull { it.value.clazz!!.isAssignableFrom(cls) }
        }?.key
    }

    fun getName(id: String) = if (id.isNotEmpty()) M.optTr("type.$id") else null

    fun setDefault(id: String, value: Any) {
        requiredType(id).value = value
    }

    fun getDefault(id: String) = if (id.isNotEmpty()) lookupType(id)?.value?.actualValue else null

    fun printable(obj: Any) = getType(obj)?.let {
        when (it) {
            STRING, BOOLEAN, INTEGER, REAL, DATETIME, DATE, TIME -> obj.toString()
            LOCALE -> (obj as Locale).displayName
            else -> null
        }
    }

    private fun requiredType(id: String): Item {
        require(id.isNotEmpty()) { "type id cannot be empty" }
        return lookupType(id) ?: throw IllegalStateException("no such type id $id")
    }

    private fun fetchType(id: String) = lookupType(id) ?: Item().apply { types[id] = this }

    private fun lookupType(id: String): Item? {
        var item = cache[id] ?: types[id]
        if (item != null) {
            return item
        }
        item = types.entries.firstOrNull { id in it.value.aliases }?.value ?: return null
        cache[id] = item
        return item
    }

    private fun initBuiltins() {
        try {
            getProperties("!jclp/value/types.properties")?.let {
                for ((key, value) in it) {
                    try {
                        mapClass(value.toString(), Class.forName(key.toString()))
                    } catch (e: ClassNotFoundException) {
                        Log.e("Variants", e) { "cannot load type class" }
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("Variants", e) { "cannot load types mapping" }
        }
    }

    private fun initDefaults() {
        setDefault(REAL, 0.0)
        setDefault(INTEGER, 0)
        setDefault(STRING, "")
        setDefault(BOOLEAN, false)
        setDefault(DATE, { LocalDate.now() })
        setDefault(TIME, { LocalTime.now() })
        setDefault(DATETIME, { LocalDateTime.now() })
        setDefault(LOCALE, { Locale.getDefault() })
        setDefault(TEXT, emptyText())
        setDefault(FLOB, emptyFlob())
    }

    private class Item {
        var clazz: Class<*>? = null

        val aliases = hashSetOf<String>()

        var value: Any? = null
    }
}

typealias VariantValidator = (String, Any) -> Unit

class VariantMap(private val validator: VariantValidator? = null) : Iterable<Pair<String, Any>>, Cloneable {
    private var values = hashMapOf<String, Any>()

    fun isNotEmpty() = values.isNotEmpty()

    fun isEmpty() = values.isEmpty()

    val names get() = values.keys

    val size get() = values.size

    operator fun set(name: String, value: Any): Any? {
        require(name.isNotEmpty()) { "name cannot be empty" }
        validator?.invoke(name, value)
        return values.put(name, value)
    }

    fun update(others: VariantMap) {
        update(others.values)
    }

    fun update(values: Map<String, Any>) {
        for ((key, value) in values) {
            set(key, value)
        }
    }

    operator fun contains(name: String) = name.isNotEmpty() && name in values

    operator fun get(name: String) = if (name.isEmpty()) null else values[name]

    fun remove(name: String) = if (name.isEmpty()) null else values.remove(name)

    override fun iterator() = values.entries.map { it.toPair() }.iterator()

    fun clear() = values.clear()

    @Suppress("UNCHECKED_CAST")
    public override fun clone(): VariantMap {
        val copy = super.clone() as VariantMap
        copy.values = values.clone() as HashMap<String, Any>
        return copy
    }

    override fun toString() = values.toString()
}
