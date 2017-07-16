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

package jem.scj.addon

import jem.scj.app.SCI
import jem.scj.app.newBook
import jem.scj.app.outParam
import jem.scj.app.saveBook
import mala.cli.CLIDelegate
import mala.cli.Command
import mala.cli.action
import mala.core.Plugin

class BookCreator : Plugin, Command {
    override val meta = mapOf("name" to "Book Creator")

    override fun init() {
        SCI.newOption("C", "create-book").action(this)
    }

    override fun execute(delegate: CLIDelegate): Int {
        val book = newBook(true)
        val path = saveBook(outParam(book)) ?: return -1
        println(path)
        return 0
    }
}
