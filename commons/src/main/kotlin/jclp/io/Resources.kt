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

package jclp.io

import java.io.File
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL
import java.util.*

const val CLASSPATH_PREFIX = "!"

fun defaultLoader(): ClassLoader = Thread.currentThread().contextClassLoader ?: ClassLoader.getSystemClassLoader()

fun getResource(uri: String, loader: ClassLoader? = null): URL? = when {
    uri.isEmpty() -> throw IllegalArgumentException("'uri' cannot be empty")
    uri.startsWith(CLASSPATH_PREFIX) -> (loader ?: defaultLoader()).getResource(uri.substring(CLASSPATH_PREFIX.length))
    else -> File(uri).takeIf(File::exists)?.toURI()?.toURL() ?: try {
        URL(uri)
    } catch (e: MalformedURLException) {
        null
    }
}

fun openResource(uri: String, loader: ClassLoader? = null, reload: Boolean = false): InputStream? {
    return getResource(uri, loader)?.openConnection()?.apply { if (reload) useCaches = false }?.getInputStream()
}

fun getProperties(uri: String, loader: ClassLoader? = null, reload: Boolean = false): Properties? {
    return openResource(uri, loader, reload)?.use { Properties().apply { load(it) } }
}
