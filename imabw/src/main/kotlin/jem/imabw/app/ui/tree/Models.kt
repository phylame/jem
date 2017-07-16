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

package jem.imabw.app.ui.tree

import jem.Chapter
import java.util.*
import javax.swing.event.EventListenerList
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.tree.TreeModel
import javax.swing.tree.TreePath

val Chapter.isRoot: Boolean get() = parent == null

fun Chapter.pathToRoot(): Array<Chapter> {
    val paths = LinkedList<Chapter>()
    var node: Chapter? = this
    while (node != null) {
        paths.addFirst(node)
        node = node.parent
    }
    return paths.toTypedArray()
}

val TreePath.chapter: Chapter? get() = lastPathComponent as? Chapter

object BookModel : TreeModel {
    var book: Chapter? = null
        set(value) {
            field = value
            onContentsChanged(value)
        }

    override fun getRoot(): Chapter? = book

    override fun isLeaf(node: Any): Boolean = !(node as Chapter).isSection

    override fun getChildCount(parent: Any): Int = (parent as Chapter).size()

    override fun getChild(parent: Any, index: Int): Chapter = (parent as Chapter).chapterAt(index)

    override fun getIndexOfChild(parent: Any, child: Any): Int = (parent as Chapter).indexOf(child as Chapter)

    override fun valueForPathChanged(path: TreePath, value: Any) {
        throw UnsupportedOperationException()
    }

    private val listeners = EventListenerList()

    override fun addTreeModelListener(l: TreeModelListener) {
        listeners.add(TreeModelListener::class.java, l)
    }

    override fun removeTreeModelListener(l: TreeModelListener) {
        listeners.remove(TreeModelListener::class.java, l)
    }

    /**
     * Invoke this method after you've inserted some chapters into
     * parent. indices should be the index of the new chapters and
     * must be sorted in ascending order.
     */
    fun onChaptersInserted(parent: Chapter?, indices: IntArray?, chapters: Array<Chapter>?) {
        if (parent != null && indices != null && indices.isNotEmpty()) {
            fireChaptersInserted(this, parent.pathToRoot(), indices, chapters)
        }
    }

    /**
     * Invoke this method after you've removed some chapters from
     * parent. indices should be the index of the removed chapters and
     * must be sorted in ascending order. And chapters should be
     * the array of the chapters chapters that were removed.
     */
    fun onChaptersRemoved(parent: Chapter?, indices: IntArray?, chapters: Array<Chapter>?) {
        if (parent != null && indices != null && indices.isNotEmpty()) {
            fireChaptersRemoved(this, parent.pathToRoot(), indices, chapters)
        }
    }

    fun onChapterUpdated(chapter: Chapter) {
        val parent = chapter.parent
        if (parent != null) {
            val index = parent.indexOf(chapter)
            if (index != -1) {
                onChapterUpdated(parent, intArrayOf(index), arrayOf(chapter))
            }
        } else if (chapter === book) {
            onChapterUpdated(chapter, null, null)
        }
    }

    /**
     * Invoke this method after you've changed how the chapters identified by
     * indices are to be represented in the tree.
     */
    fun onChapterUpdated(parent: Chapter?, indices: IntArray?, chapters: Array<Chapter>?) {
        if (parent != null) {
            if (indices != null && indices.isNotEmpty()) {
                fireChapterUpdated(this, parent.pathToRoot(), indices, chapters)
            } else if (parent === book) {
                fireChapterUpdated(this, parent.pathToRoot(), null, null)
            }
        }
    }

    /**
     * Invoke this method if you've totally changed the children of
     * book and its children's children...  This will post a
     * treeStructureChanged event.
     */
    fun onContentsChanged(parent: Chapter?) {
        if (parent != null) {
            fireContentsChanged(this, parent.pathToRoot(), null, null)
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created using the parameters passed into
     * the fire method.

     * @param source   the source of the `TreeModelEvent`; typically `this`
     * @param path     the path to the parent book that changed; use `null` to identify the book has changed
     * @param indices  the indices of the changed chapters
     * @param chapters the updated chapters
     */
    private fun fireChapterUpdated(source: Any, path: Array<Chapter>, indices: IntArray?, chapters: Array<Chapter>?) {
        postEventToListeners(source, path, indices, chapters) {
            treeNodesChanged(it)
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created using the parameters passed into
     * the fire method.

     * @param source   the source of the `TreeModelEvent`; typically `this`
     * @param path     the path to the parent book were added to
     * @param indices  the indices of the new chapters
     * @param chapters the new chapters
     */
    private fun fireChaptersInserted(source: Any, path: Array<Chapter>, indices: IntArray?, chapters: Array<Chapter>?) {
        postEventToListeners(source, path, indices, chapters) {
            treeNodesInserted(it)
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created using the parameters passed into
     * the fire method.

     * @param source   the source of the `TreeModelEvent`; typically `this`
     * @param path     the path to the parent book were removed from
     * @param indices  the indices of the removed chapters
     * @param chapters the removed chapters
     */
    private fun fireChaptersRemoved(source: Any, path: Array<Chapter>, indices: IntArray?, chapters: Array<Chapter>?) {
        postEventToListeners(source, path, indices, chapters) {
            treeNodesRemoved(it)
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created using the parameters passed into
     * the fire method.

     * @param source   the source of the `TreeModelEvent`; typically `this`
     * @param path     the path to the parent of the structure that has changed; use `null` to identify the book has changed
     * @param indices  the indices of the affected book
     * @param chapters the affected book
     */
    private fun fireContentsChanged(source: Any, path: Array<Chapter>, indices: IntArray?, chapters: Array<Chapter>?) {
        postEventToListeners(source, path, indices, chapters) {
            treeStructureChanged(it)
        }
    }

    private fun postEventToListeners(source: Any,
                                     path: Array<Chapter>,
                                     indices: IntArray?,
                                     chapters: Array<Chapter>?,
                                     action: TreeModelListener.(TreeModelEvent) -> Unit) {
        // Guaranteed to return a non-null array
        val listeners = listeners.listenerList
        var e: TreeModelEvent? = null
        // Process the listeners last to first, notifying
        // those that are interested in this event
        var i = listeners.size - 2
        while (i >= 0) {
            if (listeners[i] === TreeModelListener::class.java) {
                // Lazily create the event:
                if (e == null) {
                    e = TreeModelEvent(source, path, indices, chapters)
                }
                (listeners[i + 1] as TreeModelListener).action(e)
            }
            i -= 2
        }
    }
}

object Clipboard {
    operator fun contains(chapter: Chapter): Boolean = false

    val isCopying = true
}
