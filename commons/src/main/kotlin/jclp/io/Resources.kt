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

package jclp.io

import java.io.File
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL
import java.util.*

fun defaultClassLoader(): ClassLoader =
        Thread.currentThread().contextClassLoader ?: ClassLoader.getSystemClassLoader()

fun getResource(path: String, loader: ClassLoader? = null): URL? = when {
    path.isEmpty() -> throw IllegalArgumentException("'path' cannot be empty")
    path.startsWith('!') -> (loader ?: defaultClassLoader()).getResource(path.substring(1))
    else -> File(path).takeIf { it.exists() }?.toURI()?.toURL() ?: try {
        URL(path)
    } catch (e: MalformedURLException) {
        null
    }
}

fun openResource(path: String, loader: ClassLoader? = null, reload: Boolean = false): InputStream? =
        getResource(path, loader)?.openConnection()?.apply { if (reload) useCaches = false }?.getInputStream()

fun getProperties(path: String, loader: ClassLoader? = null, reload: Boolean = false): Properties? =
        openResource(path, loader, reload)?.use { Properties().apply { load(it) } }
