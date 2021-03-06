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

package jem.sci

import jem.Book
import jem.asBook
import jem.epm.ParserParam
import mala.App
import mala.App.tr
import mala.cli.CDelegate
import mala.cli.Command
import mala.cli.ListFetcher

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
        return openBook(ParserParam(input, format, SCI.inArguments), attaching)?.let {
            try {
                consume(it)
            } finally {
                it.cleanup()
            }
        } == true
    }
}

class ViewBook : ListFetcher("w"), BookConsumer {
    @Suppress("UNCHECKED_CAST")
    override fun consume(book: Book): Boolean {
        viewBook(book, (SCI["w"] as? List<String>) ?: listOf("all"), SCISettings.viewSettings())
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
            App.error("no book specified")
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
        book += openBook(ParserParam(input, format, SCI.inArguments), false) ?: return false
        return true
    }
}

class ExtractBook : ListFetcher("x"), BookConsumer {
    override val attaching get() = false

    @Suppress("UNCHECKED_CAST")
    override fun consume(book: Book): Boolean {
        return (SCI["x"] as? List<String> ?: return false).mapNotNull(::parseIndices).mapNotNull {
            locateChapter(book, it)
        }.map {
            val b = it.asBook()
            attachBook(b, true)
            saveBook(outParam(b))
        }.all {
            if (it != null) {
                println(tr("save.result", it))
                true
            } else false
        }
    }
}
