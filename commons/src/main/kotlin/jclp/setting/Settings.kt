package jclp.setting

import jclp.log.Log
import jclp.value.actualValue
import jclp.putAll
import jclp.text.Converters
import java.io.Reader
import java.io.Writer
import java.util.*

private typealias Predicate = (Any?) -> Boolean

interface Settings : Iterable<Pair<String, Any>> {
    fun isEnable(key: String): Boolean

    operator fun get(key: String): Any?

    fun <T : Any> get(key: String, type: Class<T>): T?

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(key: String, type: Class<T>, fallback: T) = try {
        get(key, type)?.actualValue ?: fallback
    } catch (e: Exception) {
        fallback
    }

    operator fun contains(key: String): Boolean

    operator fun set(key: String, value: Any): Any?

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

fun Settings.getInt(key: String, fallback: Int = 0) = get(key, Int::class.java, fallback)

fun Settings.getDouble(key: String, fallback: Double = 0.0) = get(key, Double::class.java, fallback)

fun Settings.getString(key: String, fallback: String = "") = get(key, String::class.java, fallback)

fun Settings.getBoolean(key: String, fallback: Boolean = false) = get(key, Boolean::class.java, fallback)

operator fun Settings.plusAssign(values: Map<String, Any>) {
    update(values)
}

operator fun Settings.plusAssign(settings: Settings) {
    update(settings)
}

abstract class AbstractSettings : Settings {
    private val definitions = HashMap<String, Definition>()

    protected abstract fun handleGet(key: String): Any?

    protected abstract fun handleSet(key: String, value: Any): Any?

    protected abstract fun <T : Any> convertValue(value: Any, type: Class<T>): T?

    override fun isEnable(key: String): Boolean = definitions[key]?.dependencies?.all {
        isEnable(it.key) && it.condition?.invoke(get(it.key)) ?: true
    } ?: true

    fun getDefinition(key: String) = definitions[key]

    fun setDefinition(key: String, definition: Definition) = definitions.put(key, definition)

    override fun get(key: String) = handleGet(key) ?: definitions[key]?.default?.actualValue

    @Suppress("UNCHECKED_CAST")
    override operator fun <T : Any> get(key: String, type: Class<T>): T? {
        val value = handleGet(key)
        return when {
            value == null -> definitions[key]?.default?.actualValue as? T
            type.isInstance(value) -> value as T
            else -> convertValue(value, type)
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

class MapSettings(values: Map<*, Any>? = null, definitions: Map<String, Definition>? = null) : AbstractSettings() {
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
