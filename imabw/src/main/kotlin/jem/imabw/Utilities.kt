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

import javafx.concurrent.Task
import mala.App
import java.io.File
import java.nio.charset.Charset

abstract class ProgressTask<V> : Task<V>() {
    init {
        messageProperty().addListener { _, _, text -> updateProgress(text) }
        setOnScheduled { showProgress() }
        setOnCancelled { hideProgress() }
        setOnSucceeded { hideProgress() }
        setOnFailed { hideProgress() }
    }

    open fun showProgress() {
        Imabw.fxApp.showProgress()
    }

    open fun updateProgress(text: String) {
        Imabw.fxApp.updateProgress(text)
    }

    open fun hideProgress() {
        Imabw.fxApp.hideProgress()
    }
}

class ReadLineTask(val file: File, val charset: Charset = Charsets.UTF_8) : ProgressTask<List<String>>() {
    init {
        setOnFailed {
            hideProgress()
            App.error("failed to read lines from $file", exception)
        }
    }

    override fun call() = file.readLines(charset)
}
