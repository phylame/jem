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

package jem.format.epub.v3

data class MediaType(val handler: String, val mediaType: String) {
    init {
        require(handler.isNotEmpty()) { "'handler' cannot be empty" }
        require(mediaType.isNotEmpty()) { "'mediaType' cannot be empty" }
    }
}

class Bindings {
    internal val items = arrayListOf<MediaType>()

    fun addItem(item: MediaType) {
        items += item
    }

    fun removeItem(item: MediaType) {
        items -= item
    }

    fun addMediaType(handler: String, mediaType: String): MediaType
            = MediaType(handler, mediaType).also { addItem(it) }
}
