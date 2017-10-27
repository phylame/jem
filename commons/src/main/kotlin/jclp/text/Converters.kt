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
    fun render(value: T): String

    fun parse(text: String): T
}

object Converters {
    private val converters = IdentityHashMap<Class<*>, Converter<*>>()

    init {
        registerDefaults()
    }

    operator fun contains(type: Class<*>) = type.canonicalType in converters

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(type: Class<T>) = converters[type.canonicalType] as Converter<T>?

    operator fun <T : Any> set(type: Class<T>, converter: Converter<T>?) = if (converter == null) {
        converters.remove(type)
    } else converters.put(type.canonicalType, converter)

    fun render(value: Any) = (value as? CharSequence)?.toString() ?: render(value, value.javaClass)

    fun <T : Any> render(value: T, type: Class<T>) = (value as? CharSequence)?.toString() ?: get(type)?.render(value)

    inline fun <reified T : Any> parse(text: String) = parse(text, T::class.java)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> parse(text: String, type: Class<T>): T? {
        return if (CharSequence::class.java == type || String::class.java == type) text as T else get(type)?.parse(text)
    }

    private fun registerDefaults() {
        Byte::class.java.let { set(it, DefaultConverter(it)) }
        Short::class.java.let { set(it, DefaultConverter(it)) }
        Int::class.java.let { set(it, DefaultConverter(it)) }
        Long::class.java.let { set(it, DefaultConverter(it)) }
        Float::class.java.let { set(it, DefaultConverter(it)) }
        Double::class.java.let { set(it, DefaultConverter(it)) }

        Class::class.java.let { set(it, DefaultConverter(it)) }
        Locale::class.java.let { set(it, DefaultConverter(it)) }
        String::class.java.let { set(it, DefaultConverter(it)) }
        Boolean::class.java.let { set(it, DefaultConverter(it)) }

        Date::class.java.let { set(it, DefaultConverter(it)) }
        LocalTime::class.java.let { set(it, DefaultConverter(it)) }
        LocalDate::class.java.let { set(it, DefaultConverter(it)) }
        LocalDateTime::class.java.let { set(it, DefaultConverter(it)) }

        set(Text::class.java, object : Converter<Text> {
            override fun parse(text: String) = textOf(text)

            override fun render(value: Text) = value.toString()
        })

        set(Flob::class.java, object : Converter<Flob> {
            override fun parse(text: String) = flobOf(text)

            override fun render(value: Flob) = throw UnsupportedOperationException()
        })
    }
}

class DefaultConverter<T>(private val type: Class<T>) : Converter<T> {
    override fun render(value: T): String = when (value) {
        is Date -> value.format(ISO_FORMAT)
        is Class<*> -> value.name
        else -> value.toString()
    }

    @Suppress("UNCHECKED_CAST")
    override fun parse(text: String): T {
        return when (type) {
            String::class.java -> text as T
            Byte::class.java -> java.lang.Byte.decode(text) as T
            Short::class.java -> java.lang.Short.decode(text) as T
            Int::class.java -> java.lang.Integer.decode(text) as T
            Long::class.java -> java.lang.Long.decode(text) as T
            Float::class.java -> java.lang.Float.valueOf(text) as T
            Double::class.java -> java.lang.Double.valueOf(text) as T
            Boolean::class.java -> java.lang.Boolean.valueOf(text) as T
            Date::class.java -> detectDate(text) as T? ?: throw IllegalArgumentException("Illegal date string: $text")
            Locale::class.java -> parseLocale(text) as T
            LocalDate::class.java -> LocalDate.parse(text, looseISODate) as T
            LocalTime::class.java -> LocalTime.parse(text, looseISOTime) as T
            LocalDateTime::class.java -> LocalDateTime.parse(text, looseISODateTime) as T
            Class::class.java -> Class.forName(text) as T
            else -> throw InternalError("Unreachable Code")
        }
    }
}
