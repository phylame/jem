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

import jclp.log.Log
import jclp.log.LogLevel

fun main(args: Array<String>) {
    Log.level = LogLevel.ALL
    println(xml {
        n("ncx") {
            attributes["id"] = "ncx"
        }
    })
}

inline fun xml(block: Node.() -> Unit) = Node("").apply {
    block()
}

class Node(val tag: String) {
    val nodes = ArrayList<Node>()

    val attributes = HashMap<String, String>()

    var text = ""

    inline fun n(tag: String, block: Node.() -> Unit) = Node(tag).apply {
        nodes += this
        block()
    }

    override fun toString(): String {
        val b = StringBuilder()
        b.append("<$tag")

        if (text.isNotEmpty()) {

        }

        return b.toString()
    }
}