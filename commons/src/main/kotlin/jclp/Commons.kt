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

package jclp

import java.io.CharArrayWriter
import java.io.PrintWriter
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

val Throwable.traceText: String
    inline get() = with(CharArrayWriter()) {
        printStackTrace(PrintWriter(this))
        toString()
    }

fun <T : CharSequence> requireNotEmpty(cs: T?): T {
    return requireNotEmpty(cs) { "Require value was empty" }
}

inline fun <T : CharSequence> requireNotEmpty(cs: T?, lazyMessage: () -> Any): T {
    if (cs == null || cs.isEmpty()) {
        throw IllegalArgumentException(lazyMessage().toString())
    }
    return cs
}

abstract class AsyncTask<out V : Any> {
    private val lock = Any()

    private lateinit var value: V

    private val isDone = AtomicBoolean()

    private val future = AtomicReference<Future<V>>()

    private val action = Callable {
        if (!isDone.get()) {
            synchronized(lock) {
                if (!isDone.get()) {
                    value = handleGet()
                    isDone.set(true)
                    future.set(null)
                }
            }
        }
        value
    }

    abstract fun handleGet(): V

    // thread safe
    fun get(): V = if (isDone.get()) value else future.get()?.get() ?: action.call()

    // non thread safe
    fun schedule(executor: ExecutorService) {
        check(future.get() == null) { "Task is already submitted in some executor" }
        future.set(executor.submit(action))
    }

    fun reset() = isDone.set(false)
}

typealias EventAction<T> = (T) -> Unit

interface Consumable {
    val isConsumed: Boolean
}

object EventBus {
    private val observers = IdentityHashMap<Class<*>, MutableList<EventAction<*>>>()

    fun <T : Any> register(type: Class<T>, action: EventAction<T>) {
        synchronized(this) {
            observers.getOrPut(type.objectType) { arrayListOf() }!! += (action)
        }
    }

    inline fun <reified T : Any> register(noinline action: EventAction<T>) {
        register(T::class.java, action)
    }

    fun <T : Any> unregistere(type: Class<T>, action: EventAction<T>) {
        synchronized(this) {
            observers[type.objectType]?.remove(action)
        }
    }

    inline fun <reified T : Any> unregistere(noinline action: EventAction<T>) {
        unregistere(T::class.java, action)
    }

    fun post(event: Any) {
        post(event, event.javaClass)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> post(event: T, type: Class<T>) {
        synchronized(this) {
            for (observer in observers[type.objectType] ?: return) {
                if ((event as? Consumable)?.isConsumed != true) {
                    (observer as EventAction<T>)(event)
                } else {
                    break
                }
            }
        }
    }
}
