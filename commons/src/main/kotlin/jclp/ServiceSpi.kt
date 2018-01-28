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

import jclp.io.defaultClassLoader
import jclp.log.Log
import java.util.*

interface KeyedService {
    val keys: Set<String>

    val name: String get() = ""
}

open class ServiceManager<S : KeyedService>(type: Class<S>, loader: ClassLoader? = null) {
    private val serviceLoader = ServiceLoader.load(type, loader ?: defaultClassLoader())

    private val localRegistry = hashMapOf<String, S>()

    private val serviceProviders = hashSetOf<S>()

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

    operator fun get(key: String) = localRegistry.getOrSet(key) {
        serviceProviders.firstOrNull { key in it.keys }
    }

    operator fun set(name: String, factory: S) {
        localRegistry[name] = factory
    }

    private fun initServices() {
        val it = serviceLoader.iterator()
        try {
            while (it.hasNext()) {
                try {
                    serviceProviders += it.next()
                } catch (e: ServiceConfigurationError) {
                    Log.e(javaClass.simpleName, e) { "in providers.next()" }
                }
            }
        } catch (e: ServiceConfigurationError) {
            Log.e(javaClass.simpleName, e) { "in providers.hasNext()" }
        }
    }
}
