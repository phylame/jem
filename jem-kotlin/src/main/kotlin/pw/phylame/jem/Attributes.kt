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

package pw.phylame.jem

import pw.phylame.jem.core.Attributes
import pw.phylame.jem.core.Chapter
import pw.phylame.jem.util.flob.Flob
import pw.phylame.jem.util.text.Text
import java.util.Date
import java.util.Locale

var Chapter.title: String get() = Attributes.getTitle(this)
    set(value) {
        Attributes.setTitle(this, value)
    }

var Chapter.cover: Flob? get() = Attributes.getCover(this)
    set(value) {
        Attributes.setCover(this, value)
    }

var Chapter.intro: Text? get() = Attributes.getIntro(this)
    set(value) {
        Attributes.setIntro(this, value)
    }

var Chapter.words: Int get() = Attributes.getWords(this)
    set(value) {
        Attributes.setWords(this, value)
    }

var Chapter.author: String get() = Attributes.getAuthor(this)
    set(value) {
        Attributes.setAuthor(this, value)
    }

var Chapter.date: Date? get() = Attributes.getDate(this)
    set(value) {
        Attributes.setDate(this, value)
    }

var Chapter.pubdate: Date? get() = Attributes.getPubdate(this)
    set(value) {
        Attributes.setPubdate(this, value)
    }

var Chapter.genre: String get() = Attributes.getGenre(this)
    set(value) {
        Attributes.setGenre(this, value)
    }

var Chapter.language: Locale? get() = Attributes.getLanguage(this)
    set(value) {
        Attributes.setLanguage(this, value)
    }

var Chapter.publisher: String get() = Attributes.getPublisher(this)
    set(value) {
        Attributes.setPublisher(this, value)
    }

var Chapter.rights: String get() = Attributes.getRights(this)
    set(value) {
        Attributes.setRights(this, value)
    }

var Chapter.state: String get() = Attributes.getState(this)
    set(value) {
        Attributes.setState(this, value)
    }

var Chapter.vendor: String get() = Attributes.getVendor(this)
    set(value) {
        Attributes.setVendor(this, value)
    }
