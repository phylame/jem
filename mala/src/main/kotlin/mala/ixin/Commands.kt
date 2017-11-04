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

package mala.ixin

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.*

interface CommandHandler {
    fun handle(command: String, source: Any): Boolean
}

annotation class Command(val name: String = "")

class CommandDispatcher(proxies: Array<out Any> = emptyArray()) : CommandHandler {
    private val handlers = LinkedList<CommandHandler>()
    private val invocations = hashMapOf<String, Invocation>()

    init {
        register(proxies)
    }

    fun reset() {
        handlers.clear()
        invocations.clear()
    }

    fun register(vararg proxies: Any) {
        for (proxy in proxies) {
            proxy.javaClass.methods.filter {
                !Modifier.isStatic(it.modifiers) && !Modifier.isAbstract(it.modifiers) && it.parameterTypes.let {
                    it.isEmpty() || it.first() == Any::class.java
                }
            }.forEach {
                val command = it.getAnnotation(Command::class.java)
                if (command != null) {
                    val invocation = Invocation(proxy, it)
                    synchronized(invocations) {
                        invocations.put(if (command.name.isNotEmpty()) command.name else it.name, invocation)
                    }
                }
            }
            if (proxy is CommandHandler) {
                synchronized(handlers) {
                    handlers += proxy
                }
                continue
            }
        }
    }

    override fun handle(command: String, source: Any): Boolean {
        val invocation = invocations[command]
        if (invocation != null) {
            return invocation.invoke(command, source)
        }
        for (handler in handlers) {
            if (handler.handle(command, source)) {
                invocations[command] = Invocation(handler)
                return true
            }
        }
        return false
    }

    override fun toString() = "CommandDispatcher(handlers=$handlers, invocations=$invocations)"

    private data class Invocation(val proxy: Any, val method: Method? = null) {
        fun invoke(command: String, source: Any) = if (method != null) {
            if (method.parameterCount == 0) method.invoke(proxy) else method.invoke(proxy, source)
            true
        } else (proxy as? CommandHandler)?.handle(command, source) == true
    }
}
