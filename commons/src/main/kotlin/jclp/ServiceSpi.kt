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

import jclp.io.defaultLoader
import jclp.log.Log
import java.util.*

interface ServiceProvider {
    val keys: Set<String>

    val name: String get() = ""
}

open class ServiceManager<T : ServiceProvider>(type: Class<T>, loader: ClassLoader? = null) {
    private val serviceLoader = ServiceLoader.load(type, loader ?: defaultLoader())
    private val localRegistry = HashMap<String, T>()
    private val serviceProviders = HashSet<T>()

    init {
        initServices()
    }

    fun reload() {
        localRegistry.clear()
        serviceProviders.clear()
        serviceLoader.reload()
        initServices()
    }

    val services get() = serviceProviders + localRegistry.values

    operator fun get(key: String) = localRegistry.getOrPut(key) {
        serviceProviders.firstOrNull { key in it.keys }
    }

    operator fun set(name: String, factory: T) {
        localRegistry.put(name, factory)
    }

    private fun initServices() {
        val it = serviceLoader.iterator()
        try {
            while (it.hasNext()) {
                try {
                    serviceProviders += it.next()
                } catch (e: ServiceConfigurationError) {
                    Log.e(javaClass.simpleName, e) { "providers.next()" }
                }
            }
        } catch (e: ServiceConfigurationError) {
            Log.e(javaClass.simpleName, e) { "providers.hasNext()" }
        }
    }
}