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

import jem.format.epub.EPUB

data class Resource(
        val id: String,
        val href: String,
        val mediaType: String,
        var fallback: String = "",
        var properties: String = "",
        var mediaOverlay: String = ""
)

class Manifest(val id: String = "") {
    internal val items = linkedMapOf<String, Resource>()

    fun addItem(item: Resource) {
        items[item.id] = item
    }

    fun remove(item: Resource) {
        items.remove(item.id)
    }
}

fun Manifest.addResource(id: String, href: String, mediaType: String, properties: String = "") =
        Resource(id, href, mediaType, properties = properties).also { addItem(it) }

fun Manifest.addNavigation(id: String, href: String): Resource =
        addResource(id, href, EPUB.MIME_XHTML, properties = EPUB.MANIFEST_NAVIGATION)

fun Manifest.addCoverImage(id: String, href: String, mediaType: String): Resource =
        addResource(id, href, mediaType, EPUB.MANIFEST_COVER_IMAGE)

