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

package jem

import jclp.VariantMap
import jclp.VariantValidator
import jclp.Variants
import jclp.flob.Flob
import jclp.io.getProperties
import jclp.log.Log
import jclp.putAll
import jclp.text.Text
import java.io.IOException
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KProperty

const val AUTHOR = "author"
const val COVER = "cover"
const val DATE = "date"
const val GENRE = "genre"
const val INTRO = "intro"
const val ISBN = "isbn"
const val KEYWORDS = "keywords"
const val LANGUAGE = "language"
const val PRICE = "price"
const val PUBDATE = "pubdate"
const val PUBLISHER = "publisher"
const val RIGHTS = "rights"
const val SERIES = "series"
const val STATE = "state"
const val TITLE = "title"
const val VENDOR = "vendor"
const val WORDS = "words"

object Attributes : VariantValidator {
    const val VALUE_SEPARATOR = ";"

    private val types = hashMapOf<String, String>()

    init {
        try {
            getProperties("!jem/attributes.properties")?.let { types.putAll(it) }
        } catch (e: IOException) {
            Log.e("Attributes", e) { "cannot load attribute mapping" }
        }
    }

    val names get() = types.keys

    fun getType(name: String) = types[name]

    fun mapType(name: String, id: String) = types.put(name, id)

    fun getName(name: String) = if (name.isNotEmpty()) M.optTr("attribute.$name") else null

    fun newAttributes() = VariantMap(this)

    override fun invoke(name: String, value: Any) {
        getType(name)?.let {
            require(Variants.getClass(it)?.isInstance(value) != false) {
                "attribute '$name' must be '$it', found '${Variants.getType(value)}'"
            }
        }
    }
}

class AttributeDelegate<T : Any?>(private val default: T) {
    @Suppress("UNCHECKED_CAST")
    operator fun getValue(chapter: Chapter, property: KProperty<*>): T {
        return chapter.attributes[property.name] as? T ?: default
    }

    operator fun setValue(chapter: Chapter, property: KProperty<*>, value: T) {
        chapter.attributes[property.name] = value!!
    }
}

var Chapter.title by AttributeDelegate("")
var Chapter.author by AttributeDelegate("")
var Chapter.cover by AttributeDelegate(null as Flob?)
var Chapter.intro by AttributeDelegate(null as Text?)
var Chapter.genre by AttributeDelegate("")
var Chapter.date by AttributeDelegate(null as LocalDate?)
var Chapter.state by AttributeDelegate("")
var Chapter.language by AttributeDelegate(null as Locale?)
var Chapter.publisher by AttributeDelegate("")
var Chapter.rights by AttributeDelegate("")
var Chapter.vendor by AttributeDelegate("")
var Chapter.words by AttributeDelegate("")
