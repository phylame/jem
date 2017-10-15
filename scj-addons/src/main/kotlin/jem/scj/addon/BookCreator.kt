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

package jem.scj.addon

import jem.scj.newBook
import jem.scj.outParam
import jem.scj.saveBook
import jem.title
import mala.App.echo
import mala.App.tr
import mala.cli.CDelegate
import mala.cli.Command
import mala.cli.action
import mala.cli.newOption

class BookCreator : SCJAddon(), Command {
    override val name = "Book Creator"

    override val description = tr("addon.creator.desc")

    override fun init() {
        newOption("C", "create-book").action(this)
    }

    override fun execute(delegate: CDelegate): Int {
        val book = newBook(true)
        if (book.title.isEmpty()) {
            val title = tr("bookCreator.untitled")
            echo(tr("bookCreator.noTitle", title))
            book.title = title
        }
        println(saveBook(outParam(book)) ?: return -1)
        return 0
    }
}
