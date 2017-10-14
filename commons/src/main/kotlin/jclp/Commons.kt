/*
 * Copyright 2017 Peng Wan <phylame@163.com>
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

package jclp

import java.io.CharArrayWriter
import java.io.PrintWriter
import java.util.*

val Throwable.traceText: String
    get() {
        return with(CharArrayWriter()) {
            printStackTrace(PrintWriter(this))
            toString()
        }
    }

typealias EventAction<T> = (T) -> Unit

object EventBus {
    private val observers = IdentityHashMap<Class<*>, MutableList<EventAction<*>>>()

    fun <T : Any> register(type: Class<T>, action: EventAction<T>) {
        synchronized(this) {
            observers.getOrPut(type.canonicalType) { arrayListOf() }!! += (action)
        }
    }

    inline fun <reified T : Any> register(noinline action: EventAction<T>) {
        register(T::class.java, action)
    }

    fun <T : Any> unregistere(type: Class<T>, action: EventAction<T>) {
        synchronized(this) {
            observers[type.canonicalType]?.remove(action)
        }
    }

    fun post(event: Any) {
        post(event, event.javaClass)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> post(event: T, type: Class<T>) {
        synchronized(this) {
            observers[type.canonicalType]?.forEach { (it as EventAction<T>)(event) }
        }
    }
}
