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

package mala.cli

import jclp.text.Converters
import mala.App
import org.apache.commons.cli.CommandLine

internal typealias Validator<T> = (T) -> Boolean

internal typealias AppContext = MutableMap<String, Any>

interface Action

interface Command : Action {
    fun execute(delegate: CDelegate): Int
}

interface Initializer : Action {
    fun perform(context: AppContext, cmd: CommandLine)
}

interface ValueFetcher<T : Any> : Initializer {
    val opt: String

    fun parse(str: String): T

    val validator: Validator<T>?

    override fun perform(context: AppContext, cmd: CommandLine) {
        val value = parse(cmd.getOptionValue(opt))
        if (validator?.invoke(value) != false) {
            context[opt] = value
        }
    }
}

class StringFetcher(override val opt: String, override val validator: Validator<String>? = null) : ValueFetcher<String> {
    override fun parse(str: String) = str
}

class TypedFetcher<T : Any>(
        private val type: Class<T>, override val opt: String, override val validator: Validator<T>? = null
) : ValueFetcher<T> {
    override fun parse(str: String) = try {
        Converters.parse(str, type) ?: App.die("cannot convert '$str' with type '$type'")
    } catch (e: RuntimeException) {
        App.die("cannot convert input '$str' with type '$type'", e)
    }
}

inline fun <reified T : Any> fetcherFor(opt: String, noinline validator: Validator<T>?): TypedFetcher<T> {
    return TypedFetcher(T::class.java, opt, validator)
}

abstract class SingleFetcher : Initializer {
    private var isPerformed = false

    protected abstract fun init(context: AppContext, cmd: CommandLine)

    override final fun perform(context: AppContext, cmd: CommandLine) {
        if (!isPerformed) {
            init(context, cmd)
            isPerformed = true
        }
    }
}

open class ListFetcher(private val opt: String) : SingleFetcher() {
    override fun init(context: AppContext, cmd: CommandLine) {
        context[opt] = cmd.getOptionValues(opt).toList()
    }
}

open class PropertiesFetcher(private val opt: String) : SingleFetcher() {
    override fun init(context: AppContext, cmd: CommandLine) {
        context[opt] = cmd.getOptionProperties(opt)
    }
}

open class ValueSwitcher(private val opt: String) : Initializer {
    override fun perform(context: AppContext, cmd: CommandLine) {
        context[opt] = true
    }
}
