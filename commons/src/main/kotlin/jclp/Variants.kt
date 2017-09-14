package jclp

import jclp.io.getProperties
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import java.util.function.Supplier
import kotlin.collections.HashMap
import kotlin.collections.HashSet

fun detectValue(value: Any) = when (value) {
    is Function0<*> -> value.invoke()
    is Supplier<*> -> value.get()
    else -> value
}

object Types {
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

    private val types = HashMap<String, Item>()
    private val cache = HashMap<String, Item>()
    private val classes = IdentityHashMap<Class<*>, String>()

    init {
        initBuiltins();
        initDefaults();
    }

    fun allTypes() = types.keys

    fun setAlias(id: String, vararg aliases: String) {
        requiredType(id).aliases += aliases
    }

    fun mapClass(id: String, clazz: Class<*>) {
        require(id.isNotEmpty()) { "type id cannot be empty" }
        fetchType(id).clazz = clazz
        classes.put(clazz, id)
        cache.remove(id)
    }

    fun getClass(id: String) = if (id.isNotEmpty()) lookupType(id)?.clazz else null

    fun getType(obj: Any) = getType(obj.javaClass)

    fun getType(clazz: Class<*>): String? {
        return classes.getOrPut(clazz, false) { cls ->
            types.entries.firstOrNull { it.value.clazz == cls }?.key
                    ?: types.entries.firstOrNull { it.value.clazz!!.isAssignableFrom(cls) }?.key
        }
    }

    fun getName(id: String) = if (id.isNotEmpty()) M.optTr("type.$id", "") else ""

    fun setDefault(id: String, value: Any) {
        requiredType(id).value = value
    }

    fun getDefault(id: String): Any? {
        if (id.isEmpty()) {
            return null
        }
        return detectValue(lookupType(id)?.value ?: return null)
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
        var props: Properties? = null
        try {
            props = getProperties("!jclp/value/types.properties")
        } catch (e: IOException) {
            Log.e("Types", "cannot load types mapping", e)
        }
        if (props == null || props.isEmpty) {
            return
        }
        for ((key, value) in props) {
            try {
                mapClass(value.toString(), Class.forName(key.toString()))
            } catch (e: ClassNotFoundException) {
                Log.e("Types", "cannot load type class", e)
            }
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
        setDefault(LEGACY_DATE, { Date() })
        setDefault(LOCALE, { Locale.getDefault() })
    }

    private class Item {
        var clazz: Class<*>? = null

        val aliases = HashSet<String>()

        var value: Any? = null
    }
}