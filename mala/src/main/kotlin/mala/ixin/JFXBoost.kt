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

package mala.ixin

import javafx.beans.binding.ObjectBinding
import javafx.beans.value.ObservableObjectValue
import javafx.beans.value.WritableValue
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.layout.Pane
import kotlin.reflect.KProperty

operator fun Pane.plusAssign(node: Node) {
    children.add(node)
}

operator fun Pane.plusAssign(elements: Collection<Node>) {
    children.addAll(elements)
}

operator fun Group.plusAssign(node: Node) {
    children.add(node)
}

operator fun Group.plusAssign(elements: Collection<Node>) {
    children.addAll(elements)
}

operator fun <T> WritableValue<T>.getValue(ref: Any, property: KProperty<*>): T? {
    return value
}

operator fun <T> WritableValue<T>.setValue(ref: Any, property: KProperty<*>, value: T?) {
    this.value = value
}

fun <T> ObservableObjectValue<T>.coalesce(vararg others: ObservableObjectValue<T>) = object : ObjectBinding<T?>() {
    init {
        super.bind(this@coalesce)
    }

    override fun dispose() {
        super.unbind(this@coalesce)
    }

    override fun computeValue(): T? {
        return this@coalesce.get() ?: others.map { it.get() }.firstOrNull()
    }
}
