/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
 *
 * This file is part of Imabw.
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

package pw.phylame.jem.imabw.app.ui.tree

import pw.phylame.jem.core.Chapter
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

val TreePath.myChapter: Chapter? get() = lastPathComponent as? Chapter

class BookModel(var book: Chapter? = null) : TreeModel {
    private val listeners = EventListenerList()

    fun update(book: Chapter) {
        this.book = book
        childrenChanged(book);
    }

    override fun getChild(parent: Any, index: Int): Chapter = (parent as Chapter).chapterAt(index)

    override fun getRoot(): Chapter? = book

    override fun isLeaf(node: Any): Boolean = !(node as Chapter).isSection

    override fun getChildCount(parent: Any): Int = (parent as Chapter).size()

    override fun getIndexOfChild(parent: Any, child: Any): Int = (parent as Chapter).indexOf(child as Chapter)

    override fun valueForPathChanged(path: TreePath, newValue: Any) {
        throw UnsupportedOperationException()
    }

    override fun addTreeModelListener(l: TreeModelListener) {
        listeners.add(TreeModelListener::class.java, l)
    }

    override fun removeTreeModelListener(l: TreeModelListener) {
        listeners.remove(TreeModelListener::class.java, l)
    }

    /**
     * Invoke this method after you've inserted some chapters into
     * parent.  indices should be the index of the new chapters and
     * must be sorted in ascending order.
     */
    fun chaptersInserted(parent: Chapter?, indices: IntArray?, children: Array<Chapter>) {
        if (parent != null && indices != null && indices.size > 0) {
            fireChaptersInserted(this, parent.pathToRoot(), indices, children)
        }
    }

    /**
     * Invoke this method after you've removed some chapters from
     * parent.  indices should be the index of the removed chapters and
     * must be sorted in ascending order. And children should be
     * the array of the children chapters that were removed.
     */
    fun chaptersRemoved(parent: Chapter?, indices: IntArray?, children: Array<Chapter>) {
        if (parent != null && indices != null) {
            fireChaptersRemoved(this, parent.pathToRoot(), indices, children)
        }
    }

    /**
     * Invoke this method after you've changed how the children identified by
     * indices are to be represented in the tree.
     */
    fun chapterUpdated(parent: Chapter?, indices: IntArray?, children: Array<Chapter>?) {
        if (parent != null) {
            if (indices != null && indices.size > 0) {
                fireChapterUpdated(this, parent.pathToRoot(), indices, children)
            } else if (parent === root) {
                fireChapterUpdated(this, parent.pathToRoot(), null, null)
            }
        }
    }

    fun chapterUpdated(chapter: Chapter) {
        val parent = chapter.parent
        if (parent != null) {
            val index = parent.indexOf(chapter)
            if (index != -1) {
                chapterUpdated(parent, intArrayOf(index), arrayOf(chapter))
            }
        } else if (chapter === root) {
            chapterUpdated(chapter, null, null)
        }
    }

    /**
     * Invoke this method if you've totally changed the children of
     * chapter and its children's children...  This will post a
     * treeStructureChanged event.
     */
    fun childrenChanged(parent: Chapter?) {
        if (parent != null) {
            fireChildrenChanged(this, parent.pathToRoot(), null, null)
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created using the parameters passed into
     * the fire method.

     * @param source   the source of the `TreeModelEvent`;
     * *                 typically `this`
     * *
     * @param path     the path to the parent chapter that changed; use
     * *                 `null` to identify the root has changed
     * *
     * @param indices  the indices of the changed chapters
     * *
     * @param children the updated chapters
     */
    private fun fireChapterUpdated(source: Any, path: Array<Chapter>, indices: IntArray?, children: Array<Chapter>?) {
        // Guaranteed to return a non-null array
        val listeners = listeners.listenerList
        var e: TreeModelEvent? = null
        // Process the listeners last to first, notifying
        // those that are interested in this event
        var i = listeners.size - 2
        while (i >= 0) {
            if (listeners[i] === TreeModelListener::class.java) {
                // Lazily create the event:
                if (e == null)
                    e = TreeModelEvent(source, path, indices, children)
                (listeners[i + 1] as TreeModelListener).treeNodesChanged(e)
            }
            i -= 2
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created using the parameters passed into
     * the fire method.

     * @param source   the source of the `TreeModelEvent`;
     * *                 typically `this`
     * *
     * @param path     the path to the parent chapter were added to
     * *
     * @param indices  the indices of the new chapters
     * *
     * @param children the new chapters
     */
    private fun fireChaptersInserted(source: Any, path: Array<Chapter>, indices: IntArray, children: Array<Chapter>) {
        // Guaranteed to return a non-null array
        val listeners = listeners.listenerList
        var e: TreeModelEvent? = null
        // Process the listeners last to first, notifying
        // those that are interested in this event
        var i = listeners.size - 2
        while (i >= 0) {
            if (listeners[i] === TreeModelListener::class.java) {
                // Lazily create the event:
                if (e == null)
                    e = TreeModelEvent(source, path, indices, children)
                (listeners[i + 1] as TreeModelListener).treeNodesInserted(e)
            }
            i -= 2
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created using the parameters passed into
     * the fire method.

     * @param source   the source of the `TreeModelEvent`;
     * *                 typically `this`
     * *
     * @param path     the path to the parent chapter were removed from
     * *
     * @param indices  the indices of the removed chapters
     * *
     * @param children the removed chapters
     */
    private fun fireChaptersRemoved(source: Any, path: Array<Chapter>, indices: IntArray, children: Array<Chapter>) {
        // Guaranteed to return a non-null array
        val listeners = listeners.listenerList
        var e: TreeModelEvent? = null
        // Process the listeners last to first, notifying
        // those that are interested in this event
        var i = listeners.size - 2
        while (i >= 0) {
            if (listeners[i] === TreeModelListener::class.java) {
                // Lazily create the event:
                if (e == null)
                    e = TreeModelEvent(source, path, indices, children)
                (listeners[i + 1] as TreeModelListener).treeNodesRemoved(e)
            }
            i -= 2
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created using the parameters passed into
     * the fire method.

     * @param source   the source of the `TreeModelEvent`;
     * *                 typically `this`
     * *
     * @param path     the path to the parent of the structure that has changed;
     * *                 use `null` to identify the root has changed
     * *
     * @param indices  the indices of the affected chapter
     * *
     * @param children the affected chapter
     */
    private fun fireChildrenChanged(source: Any, path: Array<Chapter>, indices: IntArray?, children: Array<Chapter>?) {
        // Guaranteed to return a non-null array
        val listeners = listeners.listenerList
        var e: TreeModelEvent? = null
        // Process the listeners last to first, notifying
        // those that are interested in this event
        var i = listeners.size - 2
        while (i >= 0) {
            if (listeners[i] === TreeModelListener::class.java) {
                // Lazily create the event:
                if (e == null)
                    e = TreeModelEvent(source, path, indices, children)
                (listeners[i + 1] as TreeModelListener).treeStructureChanged(e)
            }
            i -= 2
        }
    }

}
