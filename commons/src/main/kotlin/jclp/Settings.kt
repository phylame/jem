package jclp

import java.util.*


private typealias Predicate = (Any) -> Boolean

interface Settings : Iterable<Pair<String, Any>> {
    fun isEnable(key: String): Boolean

    operator fun get(key: String): Any?

    fun <T> get(key: String, type: Class<T>): T?

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String, type: Class<T>, fallback: T) = try {
        get(key, type)?.let { detectValue(this) as T } ?: fallback
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
    var definitions = HashMap<String, Definition>()

    protected abstract fun handleGet(key: String): Any?

    protected abstract fun handleSet(key: String, value: Any): Any

    protected abstract fun <T> convertValue(value: Any, type: Class<T>): T

    override fun isEnable(key: String): Boolean {
        if (definitions.isEmpty()) {
            return true
        }
        val definition = definitions[key] ?: return true
        val dependencies = definition.dependencies
        if (isEmpty(dependencies)) {
            return true
        }
        for (dependency in dependencies) {
            if (!isEnable(dependency.getKey()) || !dependency.getCondition().test(get(dependency.getKey()))) {
                return false
            }
        }
        return true
    }

    fun setDefinition(key: String, definition: Definition) {
        definitions.put(key, definition)
    }

    fun getDefinition(key: String): Definition {
        return definitions[key]
    }

    protected fun getDefault(key: String): Any? {
        val definition = definitions[key]
        return if (definition != null) Values.get(definition.getDefaults()) else null
    }

    protected fun getType(key: String): Class<*>? {
        val definition = definitions[key]
        return definition?.type
    }

    override fun get(key: String): Any? {
        val value = handleGet(key)
        return value ?: getDefault(key)
    }

    override operator fun <T> get(key: String, @NonNull type: Class<T>): T? {
        val value = handleGet(key)
        return if (value == null) {
            getDefault(key) as T?
        } else if (type.isInstance(value)) {
            value as T?
        } else {
            convertValue(value, type)
        }
    }

    override fun set(key: String, @NonNull value: Any): Any? {
        val type = getType(key)
        require(type == null || type.isInstance(value), "%s require %s", key, type)
        return handleSet(key, value)
    }

    override fun contains(key: String): Boolean {
        return handleGet(key) != null
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