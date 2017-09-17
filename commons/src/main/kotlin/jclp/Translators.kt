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

import jclp.io.contextClassLoader
import jclp.io.openResource
import jclp.text.or
import java.nio.file.Files
import java.nio.file.Paths
import java.text.MessageFormat
import java.util.*
import java.util.Locale
import kotlin.collections.LinkedHashMap
import kotlin.collections.LinkedHashSet

interface Translator {
    fun tr(key: String): String

    fun optTr(key: String, default: String) = try {
        tr(key) or default
    } catch (e: MissingResourceException) {
        default
    }

    fun tr(key: String, vararg args: Any): String = MessageFormat.format(tr(key), *args)

    fun optTr(key: String, fallback: String, vararg args: Any): String = MessageFormat.format(optTr(key, fallback), *args)

    fun attach(translators: Collection<Translator>) {
        throw UnsupportedOperationException()
    }

    fun detach(translators: Collection<Translator>) {
        throw UnsupportedOperationException()
    }
}

fun Translator.attach(vararg translators: Translator) {
    attach(translators.asList())
}

fun Translator.detach(vararg translators: Translator) {
    detach(translators.asList())
}

abstract class AbstractTranslator : Translator {
    protected abstract fun handleGet(key: String): String?

    override fun tr(key: String): String {
        return try {
            handleGet(key) ?: throw MissingResourceException(null, javaClass.name, key)
        } catch (e: MissingResourceException) {
            for (translator in attachments) {
                try {
                    return translator.tr(key)
                } catch (ignored: MissingResourceException) {
                }
            }
            throw e
        }
    }

    private val attachments = LinkedHashSet<Translator>()

    override fun attach(translators: Collection<Translator>) {
        attachments += translators
    }

    override fun detach(translators: Collection<Translator>) {
        attachments -= translators
    }
}

private const val ERR_NO_TRANSLATOR = "translator is not initialized"

open class TranslatorWrapper : Translator {
    protected var translator: Translator? = null

    override fun tr(key: String) = translator?.tr(key) ?: throw IllegalStateException(ERR_NO_TRANSLATOR)

    override fun optTr(key: String, default: String) = translator?.optTr(key, default) ?: default

    override fun tr(key: String, vararg args: Any): String {
        return translator?.tr(key, *args) ?: throw IllegalStateException(ERR_NO_TRANSLATOR)
    }

    override fun optTr(key: String, fallback: String, vararg args: Any): String {
        return translator?.optTr(key, fallback, *args) ?: MessageFormat.format(fallback, *args)
    }

    override fun attach(translators: Collection<Translator>) {
        translator?.attach(translators)
    }

    override fun detach(translators: Collection<Translator>) {
        translator?.detach(translators)
    }
}

fun ResourceBundle.toTranslator() = object : AbstractTranslator() {
    override fun handleGet(key: String) = getString(key)
}

fun Map<String, String>.toTranslator() = object : AbstractTranslator() {
    override fun handleGet(key: String) = get(key)
}

open class Linguist(
        private val name: String,
        private val locale: Locale? = null,
        private val loader: ClassLoader? = null,
        private val isDummy: Boolean = true
) : AbstractTranslator() {
    override fun handleGet(key: String): String = bundle.getString(key)

    private val bundle: ResourceBundle by lazy {
        try {
            ResourceBundle.getBundle(name, locale ?: Locale.getDefault(), loader ?: contextClassLoader(), ResourceControl)
        } catch (e: MissingResourceException) {
            if (isDummy) DummyBundle else {
                throw e
            }
        }
    }

    private object ResourceControl : ResourceBundle.Control() {
        override fun newBundle(baseName: String, locale: Locale, format: String, loader: ClassLoader, reload: Boolean): ResourceBundle {
            if (format == "java.properties") {
                val stream = openResource(toBundleName(baseName, locale) + ".properties", loader, reload)
                if (stream != null) {
                    return PropertyResourceBundle(stream)
                }
            }
            return super.newBundle(baseName, locale, format, loader, reload)
        }
    }

    private object DummyBundle : ResourceBundle() {
        override fun handleGetObject(key: String) = null

        override fun getKeys(): Enumeration<String> = Collections.emptyEnumeration<String>()
    }
}

class TranslatorHelper(private val path: String, private val filter: ((String) -> Boolean)? = null) : Translator {
    private val values = LinkedHashMap<String, String>()

    fun sync() {
        val separator = System.lineSeparator()
        Files.newBufferedWriter(Paths.get(path)).use {
            for ((key, value) in values) {
                it.append(key)
                        .append("=")
                        .append(value)
                        .append(separator)
            }
        }
    }

    override fun tr(key: String) = if (filter?.invoke(key) == false) {
        throw MissingResourceException(key, javaClass.name, key)
    } else {
        values.getOrPut(key) { it -> it }!!
    }
}
