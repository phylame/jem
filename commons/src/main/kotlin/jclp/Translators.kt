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

import jclp.io.defaultLoader
import jclp.io.openResource
import java.io.File
import java.text.MessageFormat
import java.util.*
import java.util.Locale
import kotlin.collections.LinkedHashMap

fun parseLocale(tag: String): Locale = Locale.forLanguageTag(tag.replace('_', '-'))

interface Translator {
    fun tr(key: String): String

    fun optTr(key: String) = try {
        tr(key).takeIf(String::isNotEmpty)
    } catch (e: MissingResourceException) {
        null
    }

    fun tr(key: String, vararg args: Any?): String = MessageFormat.format(tr(key), *args)

    fun optTr(key: String, fallback: String, vararg args: Any?): String = (optTr(key) ?: fallback).let {
        MessageFormat.format(it, *args)
    }

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

private const val ERR_NO_TRANSLATOR = "translator is not initialized"

open class TranslatorWrapper : Translator {
    var translator: Translator? = null

    override fun tr(key: String) = translator?.tr(key) ?: throw IllegalStateException(ERR_NO_TRANSLATOR)

    override fun optTr(key: String): String? = translator?.optTr(key)

    override fun tr(key: String, vararg args: Any?): String {
        return translator?.tr(key, *args) ?: throw IllegalStateException(ERR_NO_TRANSLATOR)
    }

    override fun optTr(key: String, fallback: String, vararg args: Any?): String {
        return translator?.optTr(key, fallback, *args) ?: MessageFormat.format(fallback, *args)
    }

    override fun attach(translators: Collection<Translator>) {
        translator?.attach(translators)
    }

    override fun detach(translators: Collection<Translator>) {
        translator?.detach(translators)
    }
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

    private val attachments = linkedSetOf<Translator>()

    override fun attach(translators: Collection<Translator>) {
        attachments += translators
    }

    override fun detach(translators: Collection<Translator>) {
        attachments -= translators
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
    override fun handleGet(key: String): String = getBundle().getString(key)

    private fun getBundle(): ResourceBundle = try {
        ResourceBundle.getBundle(name, locale ?: Locale.getDefault(), loader ?: defaultLoader(), ResourceControl)
    } catch (e: MissingResourceException) {
        if (isDummy) DummyBundle else throw e
    }

    private object ResourceControl : ResourceBundle.Control() {
        override fun newBundle(baseName: String, locale: Locale, format: String, loader: ClassLoader, reload: Boolean): ResourceBundle {
            if (format == "java.properties") {
                openResource(toBundleName(baseName, locale) + ".properties", loader, reload)?.let {
                    return PropertyResourceBundle(it)
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

class TranslatorHelper(private val path: String, private val filter: ((String) -> Boolean)? = null) : AbstractTranslator() {
    private val values = LinkedHashMap<String, String>()

    override fun handleGet(key: String) = if (filter?.invoke(key) != false) values.getOrPut(key) { it -> it } else null

    fun sync() {
        val separator = System.lineSeparator()
        File(path).bufferedWriter().use {
            for ((key, value) in values) {
                it.append(key).append("=").append(value).append(separator)
            }
        }
    }
}
