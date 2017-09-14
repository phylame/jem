package jclp

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

interface Converter<T> {
    fun render(obj: T): String

    fun parse(str: String): T
}

object Converters {
    private val converters = IdentityHashMap<Class<*>, Converter<*>>()

    init {
        registerDefaults()
    }

    operator fun contains(type: Class<*>) = type.canonicalType in converters

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(type: Class<T>) = converters[type.canonicalType] as Converter<T>?

    inline fun <reified T : Any> set(converter: Converter<T>?) = set(T::class.java, converter)

    operator fun <T : Any> set(type: Class<T>, converter: Converter<T>?) = converters.put(type.canonicalType, converter)

    inline fun <reified T : Any> render(obj: T) = render(obj, T::class.java)

    inline fun <reified T : Any> parse(str: String) = parse(str, T::class.java)

    fun <T : Any> render(obj: T, type: Class<T>) = (obj as? CharSequence)?.toString() ?: get(type.canonicalType)?.render(obj)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> parse(str: String, type: Class<T>): T? {
        return if (CharSequence::class.java.isAssignableFrom(type)) str as T else get(type.canonicalType)?.parse(str)
    }

    private fun registerDefaults() {
        Class::class.java.let { set(it, DefaultConverter(it)) }
        Locale::class.java.let { set(it, DefaultConverter(it)) }
        String::class.java.let { set(it, DefaultConverter(it)) }
        Date::class.java.let { set(it, DefaultConverter(it)) }
        LocalTime::class.java.let { set(it, DefaultConverter(it)) }
        LocalDate::class.java.let { set(it, DefaultConverter(it)) }
        LocalDateTime::class.java.let { set(it, DefaultConverter(it)) }

        Byte::class.java.let { set(it, DefaultConverter(it)) }
        Short::class.java.let { set(it, DefaultConverter(it)) }
        Int::class.java.let { set(it, DefaultConverter(it)) }
        Long::class.java.let { set(it, DefaultConverter(it)) }
        Float::class.java.let { set(it, DefaultConverter(it)) }
        Double::class.java.let { set(it, DefaultConverter(it)) }
    }
}

class DefaultConverter<T>(private val type: Class<T>) : Converter<T> {
    override fun render(obj: T): String = when (obj) {
        is Date -> obj.format(ISO_DATE_FORMAT)
        is Class<*> -> (obj as Class<*>).name
        else -> obj.toString()
    }

    @Suppress("UNCHECKED_CAST")
    override fun parse(str: String): T {
        return when (type) {
            String::class.java -> str as T
            Byte::class.java -> java.lang.Byte.decode(str) as T
            Short::class.java -> java.lang.Short.decode(str) as T
            Int::class.java -> Integer.decode(str) as T
            Long::class.java -> java.lang.Long.decode(str) as T
            Float::class.java -> java.lang.Float.valueOf(str) as T
            Double::class.java -> java.lang.Double.valueOf(str) as T
            Boolean::class.java -> java.lang.Boolean.valueOf(str) as T
            Date::class.java -> str.toDate("yyyy-M-d H:m:s", "yyyy-M-d", "H:m:s") as T? ?: throw IllegalArgumentException("Illegal date string: $str")
            Locale::class.java -> Locale.forLanguageTag(str) as T
            LocalDate::class.java -> LocalDate.parse(str, LOOSE_ISO_DATE) as T
            LocalTime::class.java -> LocalTime.parse(str, LOOSE_ISO_TIME) as T
            LocalDateTime::class.java -> LocalDateTime.parse(str, LOOSE_ISO_DATE_TIME) as T
            Class::class.java -> Class.forName(str) as T
            else -> throw IllegalStateException("Unreachable code")
        }
    }
}
