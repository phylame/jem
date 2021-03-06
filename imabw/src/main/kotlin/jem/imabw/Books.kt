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

package jem.imabw

import javafx.stage.Window
import jclp.setting.MapSettings
import jclp.text.Text
import jclp.text.or
import jem.Book
import jem.JemException
import jem.epm.*
import jem.imabw.ui.debug
import jem.title
import mala.App
import mala.App.tr

fun defaultMakerSettings() = MapSettings().apply {
    this[MAKER_OVERWRITE_KEY] = true
    this[MAKER_VDM_COMMENT_KEY] = "generated by ${Imabw.name} v${Imabw.version}"
}

fun loadBook(param: ParserParam) = EpmManager.readBook(param) ?: throw UnknownEpmException()

fun makeBook(param: MakerParam) = EpmManager.writeBook(param) ?: throw UnknownEpmException()

class UnknownEpmException : JemException("")

open class LoadBookTask(val param: ParserParam, private val window: Window = Imabw.topWindow) : ProgressTask<Book>() {
    init {
        setOnRunning {
            updateProgress(tr("jem.loadBook.hint", param.path))
        }
        setOnSucceeded {
            Imabw.message(tr("jem.openBook.success", param.path))
            hideProgress()
        }
        setOnFailed {
            hideProgress()
            debug(tr("d.openBook.title"), tr("jem.openBook.failure", param.path), exception, window)
        }
    }

    override fun call() = loadBook(param)
}

open class MakeBookTask(val param: MakerParam, private val window: Window = Imabw.topWindow) : ProgressTask<String>() {
    init {
        setOnRunning {
            updateProgress(tr("jem.makeBook.hint", param.book.title, param.actualPath))
        }
        setOnSucceeded {
            Imabw.message(tr("jem.saveBook.success", param.book.title, param.actualPath))
            hideProgress()
        }
        setOnFailed {
            hideProgress()
            debug(tr("d.saveBook.title"), tr("jem.saveBook.failure", param.actualPath), exception, window)
        }
    }

    override fun call() = makeBook(param)
}

open class LoadTextTask(private val text: Text, private val hint: String = "") : ProgressTask<String>() {
    init {
        setOnRunning {
            updateProgress(hint or { tr("misc.progress.hint") })
        }
        setOnFailed {
            hideProgress()
            App.error("failed to load text", exception)
        }
    }

    override fun call() = text.toString()
}
