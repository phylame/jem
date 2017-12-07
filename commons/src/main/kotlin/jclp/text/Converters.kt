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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

interface Converter<T> {
    fun render(value: T): String

    fun parse(text: String): T
}

object ConverterManager {
    private val converters = IdentityHashMap<Class<*>, Converter<*>>()

    init {
        registerDefaults()
    }

    operator fun contains(type: Class<*>) = type.objectType in converters

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(type: Class<T>) = converters[type.objectType] as Converter<T>?

    operator fun <T : Any> set(type: Class<T>, converter: Converter<T>?) = if (converter != null) {
        converters.put(type.objectType, converter)
    } else {
        converters.remove(type)
    }

    fun render(value: Any) = (value as? CharSequence)?.toString() ?: render(value, value.javaClass)

    fun <T : Any> render(value: T, type: Class<T>) = if (CharSequence::class.java.isAssignableFrom(type)) {
        value.toString()
    } else {
        get(type)?.render(value)
    }

    inline fun <reified T : Any> parse(text: String) = parse(text, T::class.java)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> parse(text: String, type: Class<T>): T? {
        return if (CharSequence::class.java == type || String::class.java == type) {
            text as T
        } else {
            get(type)?.parse(text)
        }
    }

    private fun registerDefaults() {
        Byte::class.java.let { set(it, DefaultConverter(it)) }
        Short::class.java.let { set(it, DefaultConverter(it)) }
        Int::class.java.let { set(it, DefaultConverter(it)) }
        Long::class.java.let { set(it, DefaultConverter(it)) }
        Float::class.java.let { set(it, DefaultConverter(it)) }
        Double::class.java.let { set(it, DefaultConverter(it)) }

        Locale::class.java.let { set(it, DefaultConverter(it)) }
        String::class.java.let { set(it, DefaultConverter(it)) }
        Boolean::class.java.let { set(it, DefaultConverter(it)) }

        Date::class.java.let { set(it, DefaultConverter(it)) }
        LocalTime::class.java.let { set(it, DefaultConverter(it)) }
        LocalDate::class.java.let { set(it, DefaultConverter(it)) }
        LocalDateTime::class.java.let { set(it, DefaultConverter(it)) }
    }
}

private class DefaultConverter<T>(val type: Class<T>) : Converter<T> {
    override fun render(value: T): String = when (value) {
        is Date -> value.format(ISO_DATE_TIME_FORMAT)
        else -> value.toString()
    }

    @Suppress("UNCHECKED_CAST")
    override fun parse(text: String) = when (type) {
        String::class.java -> text as T
        Byte::class.java -> java.lang.Byte.decode(text) as T
        Short::class.java -> java.lang.Short.decode(text) as T
        Int::class.java -> java.lang.Integer.decode(text) as T
        Long::class.java -> java.lang.Long.decode(text) as T
        Float::class.java -> java.lang.Float.valueOf(text) as T
        Double::class.java -> java.lang.Double.valueOf(text) as T
        Boolean::class.java -> java.lang.Boolean.valueOf(text) as T
        Locale::class.java -> parseLocale(text) as T
        LocalDate::class.java -> LocalDate.parse(text, looseISODate) as T
        LocalTime::class.java -> LocalTime.parse(text, looseISOTime) as T
        LocalDateTime::class.java -> LocalDateTime.parse(text, looseISODateTime) as T
        Date::class.java -> detectDate(text) as T? ?: throw IllegalArgumentException("Illegal date string: $text")
        else -> throw InternalError("Unreachable Code")
    }
}
