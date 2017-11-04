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

package jclp.setting

import jclp.text.or
import java.time.LocalDate
import java.time.LocalTime
import kotlin.reflect.KProperty

interface Settings : Iterable<MutableMap.MutableEntry<String, Any>> {
    fun isEnable(key: String): Boolean

    operator fun set(key: String, value: Any): Any?

    fun update(settings: Settings) {
        settings.forEach { set(it.key, it.value) }
    }

    fun update(values: Map<String, Any>) {
        values.forEach { set(it.key, it.value) }
    }

    operator fun get(key: String): Any?

    fun <T : Any> get(key: String, type: Class<T>): T?

    operator fun contains(key: String): Boolean

    fun remove(key: String): Any?

    fun clear()
}

fun Settings.getInt(key: String) = get(key, Int::class.java)

fun Settings.getDouble(key: String) = get(key, Double::class.java)

fun Settings.getString(key: String) = get(key, String::class.java)

fun Settings.getBoolean(key: String) = get(key, Boolean::class.java)

fun Settings.getDate(key: String) = get(key, LocalDate::class.java)

fun Settings.getTime(key: String) = get(key, LocalTime::class.java)

class SettingsDelegate<T : Any>(private val type: Class<T>, private val default: T, private val key: String = "") {
    @Suppress("UNCHECKED_CAST")
    operator fun getValue(settings: Settings, property: KProperty<*>): T {
        val key = key or { property.name }
        val value = settings.get(key, type)
        if (value == null) {
            settings[key] = default
            return default
        }
        return value
    }

    operator fun setValue(settings: Settings, property: KProperty<*>, value: T) {
        settings[key or { property.name }] = value
    }
}

inline fun <reified T : Any> Settings.delegate(default: T, key: String = ""): SettingsDelegate<T> {
    return SettingsDelegate(T::class.java, default, key)
}
