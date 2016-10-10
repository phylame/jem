/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
 *
 * This file is part of Imabw.
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

package pw.phylame.jem.imabw.app

import pw.phylame.jem.core.Book
import pw.phylame.jem.core.Chapter
import pw.phylame.qaf.core.App
import pw.phylame.qaf.ixin.Command

object Manager {
    @Command
    fun openFile() {
        val book = Book("Example")
        for (i in 1..10) {
            val ch = Chapter("Chapter $i")
            if (i % 3 == 0) {
                ch.append(Chapter("Chapter $i.1"))
            }
            book.append(ch)
        }
        Imabw.form.tree.updateBook(book)
    }

    @Command
    fun exitApp() {
        App.exit(0)
    }
}

object Worker {

}
