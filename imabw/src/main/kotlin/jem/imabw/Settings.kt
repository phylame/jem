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

import jclp.setting.delegate
import mala.AppSettings
import mala.MalaSettings

object GeneralSettings : AppSettings() {
    var enableHistory by delegate(true, "app.history.enable")

    var historyLimit by delegate(100, "app.history.limit")
}

object UISettings : MalaSettings("config/ui.ini") {
    var formX by delegate(251.0, "form.x")
    var formY by delegate(93.0, "form.y")
    var formWidth by delegate(1080.0, "form.width")
    var formHeight by delegate(670.0, "form.height")
    var formMaximized by delegate(false, "form.maximized")
    var formFullScreen by delegate(false, "form.fullScreen")
}
