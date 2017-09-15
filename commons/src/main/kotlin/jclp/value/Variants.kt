package jclp.value

import jclp.M
import jclp.getOrPut
import jclp.io.getProperties
import jclp.log.Log
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import java.util.function.Supplier
import kotlin.collections.HashSet

val <T : Any> Class<T>.canonicalType
    get() = if (isPrimitive) kotlin.javaObjectType else this

val Any?.actualValue
    get() = when (this) {
        is Function0<*> -> invoke()
        is Supplier<*> -> get()
        is Lazy<*> -> value
        else -> this
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
    const val TEXT = "text"
    const val FLOB = "file"

    private val types = HashMap<String, Item>()
    private val cache = HashMap<String, Item>()
    private val classes = IdentityHashMap<Class<*>, String>()

    init {
        initBuiltins();
        initDefaults();
    }

    val allTypes get() = types.keys

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
        return classes.getOrPut(clazz) { cls ->
            types.entries.firstOrNull { it.value.clazz == cls }?.key
                    ?: types.entries.firstOrNull { it.value.clazz!!.isAssignableFrom(cls) }?.key
        }
    }

    fun getName(id: String) = if (id.isNotEmpty()) M.optTr("type.$id", "") else ""

    fun setDefault(id: String, value: Any) {
        requiredType(id).value = value
    }

    fun getDefault(id: String) = if (id.isNotEmpty()) lookupType(id)?.value?.actualValue else null

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
            Log.e("Types", e) { "cannot load types mapping" }
        }
        if (props == null || props.isEmpty) {
            return
        }
        for ((key, value) in props) {
            try {
                mapClass(value.toString(), Class.forName(key.toString()))
            } catch (e: ClassNotFoundException) {
                Log.e("Types", e) { "cannot load type class" }
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
        setDefault(TEXT, Text.empty())
        setDefault(FLOB, Flob.empty())
    }

    private class Item {
        var clazz: Class<*>? = null

        val aliases = HashSet<String>()

        var value: Any? = null
    }
}

private typealias Validator = (String, Any) -> Unit

class VariantMap(private val validator: Validator? = null) : Iterable<Pair<String, Any>>, Cloneable {
    private var values = HashMap<String, Any>()

    val size get() = values.size

    val names get() = values.keys

    operator fun set(name: String, value: Any): Any? {
        require(name.isNotEmpty()) { "name cannot be empty" }
        validator?.invoke(name, value)
        return values.put(name, value)
    }

    operator fun plusAssign(others: VariantMap) {
        plusAssign(others.values)
    }

    operator fun plusAssign(values: Map<String, Any>) {
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
