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

package jem.scj.app

import jem.Book
import jem.epm.EpmInParam
import mala.cli.CDelegate
import mala.cli.Command
import mala.cli.ValuesFetcher
import mala.core.App.tr

interface InputProcessor {
    fun process(input: String, format: String): Boolean
}

interface ProcessorCommand : Command, InputProcessor {
    override fun execute(delegate: CDelegate): Int = SCI.processInputs(this)
}

interface BookConsumer : ProcessorCommand {
    val attaching get() = true

    fun consume(book: Book): Boolean

    override fun process(input: String, format: String): Boolean {
        val book: Book = openBook(EpmInParam(input, format, SCI.inArguments), attaching) ?: return false
        val state: Boolean
        try {
            state = consume(book)
        } finally {
            book.cleanup()
        }
        return state
    }
}

class ViewBook : ValuesFetcher("w"), BookConsumer {
    @Suppress("UNCHECKED_CAST")
    override fun consume(book: Book): Boolean {
        viewBook(book, (SCI.context["w"] as? List<String>) ?: listOf("all"), SCISettings.asConfig())
        return true
    }
}

class ConvertBook : BookConsumer {
    override fun consume(book: Book): Boolean {
        val path = saveBook(outParam(book)) ?: return false
        println(tr("save.result", path))
        return true
    }
}

class JoinBook : Command, InputProcessor {
    private val book = Book()

    override fun execute(delegate: CDelegate): Int {
        var code = SCI.processInputs(this)
        if (!book.isSection) { // no input books
            return -1
        }
        attachBook(book, true)
        val path = saveBook(outParam(book))
        if (path != null) {
            println(tr("save.result", path))
        } else {
            code = -1
        }
        book.cleanup()
        return code
    }

    override fun process(input: String, format: String): Boolean {
        book.append(openBook(EpmInParam(input, format, SCI.inArguments), false) ?: return false)
        return true
    }
}

class ExtractBook : ValuesFetcher("x"), BookConsumer {
    override val attaching get() = false

    @Suppress("UNCHECKED_CAST")
    override fun consume(book: Book): Boolean {
        return (SCI.context["x"] as? List<String> ?: return false).mapNotNull(::getIndices).mapNotNull {
            locateChapter(book, it)
        }.map {
            val b = Book(it)
            attachBook(b, true)
            saveBook(outParam(b))
        }.all {
            if (it != null) {
                println(tr("save.result", it))
                true
            } else
                false
        }
    }
}
