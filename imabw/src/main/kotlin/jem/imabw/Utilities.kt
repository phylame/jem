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

package jem.imabw

import javafx.beans.value.WeakChangeListener
import javafx.concurrent.Task
import javafx.concurrent.Worker

abstract class ProgressTask<V> : Task<V>() {
    init {
        stateProperty().addListener(WeakChangeListener { _, _, state ->
            when (state) {
                Worker.State.SCHEDULED -> Imabw.fxApp.showProgress()
                Worker.State.SUCCEEDED, Worker.State.CANCELLED, Worker.State.FAILED -> Imabw.fxApp.hideProgress()
                else -> Unit
            }
        })
    }
}

fun Task<*>.execute() {
    Imabw.submit(this)
}
