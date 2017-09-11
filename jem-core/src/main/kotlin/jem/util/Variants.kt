package jem.util

import jclp.getOrPut
import java.util.*
import java.util.function.Supplier
import kotlin.collections.HashMap

object Variants {
    const val REAL = "real"
    const val INTEGER = "int"
    const val BOOLEAN = "bool"
    const val STRING = "str"
    const val FLOB = "file"
    const val TEXT = "text"
    const val LOCALE = "locale"
    const val DATE = "date"
    const val TIME = "time"
    const val DATETIME = "datetime"

    private val typeDefaults = HashMap<String, Any>()
    private val typeMappings = HashMap<String, Class<*>>()
    private val classMappings = IdentityHashMap<Class<*>, String>()

    val types get() = typeMappings.keys

    fun mapClass(type: String, clazz: Class<*>) {
        require(type.isNotEmpty()) { "type cannot be empty" }
        typeMappings[type] = clazz
        classMappings[clazz] = type
    }

    fun getClass(type: String) = if (type.isNotEmpty()) typeMappings[type] else null

    fun getType(value: Any) = classMappings.getOrPut(value.javaClass) {
        for ((k, v) in classMappings) {
            if (k.isAssignableFrom(it)) {
                return@getOrPut v
            }
        }
        null
    }

    fun setDefault(type: String, value: Any) {
        require(type.isNotEmpty()) { "type cannot be empty" }
        typeDefaults[type] = value
    }

    fun getDefault(type: String): Any? {
        if (type.isEmpty()) {
            return null
        }
        return typeDefaults[type]?.let {
            when (it) {
                is Function0<*> -> return it.invoke()
                is Supplier<*> -> return it.get()
                else -> it
            }
        }
    }
}

