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

package jclp.text

import jclp.*
import jclp.flob.Flob
import jclp.flob.flobOf
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

    operator fun <T : Any> set(type: Class<T>, converter: Converter<T>?) = converters.put(type.canonicalType, converter)

    fun render(obj: Any) = (obj as? CharSequence)?.toString() ?: render(obj, obj.javaClass)

    fun <T : Any> render(obj: T, type: Class<T>) = (obj as? CharSequence)?.toString() ?: get(type)?.render(obj)

    inline fun <reified T : Any> parse(str: String) = parse(str, T::class.java)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> parse(str: String, type: Class<T>): T? {
        return if (CharSequence::class.java == type || String::class.java == type) str as T else get(type)?.parse(str)
    }

    private fun registerDefaults() {
        Class::class.java.let { set(it, DefaultConverter(it)) }
        Locale::class.java.let { set(it, DefaultConverter(it)) }
        String::class.java.let { set(it, DefaultConverter(it)) }
        Boolean::class.java.let { set(it, DefaultConverter(it)) }
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

        set(Text::class.java, object : Converter<Text> {
            override fun parse(str: String) = textOf(str)

            override fun render(obj: Text) = throw UnsupportedOperationException()
        })

        set(Flob::class.java, object : Converter<Flob> {
            override fun parse(str: String) = flobOf(str)

            override fun render(obj: Flob) = throw UnsupportedOperationException()
        })
    }
}

class DefaultConverter<T>(private val type: Class<T>) : Converter<T> {
    override fun render(obj: T): String = when (obj) {
        is Date -> obj.format(ISO_FORMAT)
        is Class<*> -> (obj as Class<*>).name
        else -> obj.toString()
    }

    @Suppress("UNCHECKED_CAST")
    override fun parse(str: String): T {
        return when (type) {
            String::class.java -> str as T
            Byte::class.java -> java.lang.Byte.decode(str) as T
            Short::class.java -> java.lang.Short.decode(str) as T
            Int::class.java -> java.lang.Integer.decode(str) as T
            Long::class.java -> java.lang.Long.decode(str) as T
            Float::class.java -> java.lang.Float.valueOf(str) as T
            Double::class.java -> java.lang.Double.valueOf(str) as T
            Boolean::class.java -> java.lang.Boolean.valueOf(str) as T
            Date::class.java -> detectDate(str) as T? ?: throw IllegalArgumentException("Illegal date string: $str")
            Locale::class.java -> parseLocale(str) as T
            LocalDate::class.java -> LocalDate.parse(str, looseISODate) as T
            LocalTime::class.java -> LocalTime.parse(str, looseISOTime) as T
            LocalDateTime::class.java -> LocalDateTime.parse(str, looseISODateTime) as T
            Class::class.java -> Class.forName(str) as T
            else -> throw IllegalStateException("Unreachable code")
        }
    }
}
