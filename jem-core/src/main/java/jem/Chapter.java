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

package jem;

import jclp.function.Consumer;
import jclp.log.Log;
import jem.util.Hierarchial;
import jem.util.VariantMap;
import jem.util.text.Text;
import lombok.*;

import java.util.*;

import static jclp.util.Validate.require;

/**
 * The chapter/section in Jem book models.
 * <p>
 * A chapter includes several parts:
 * </p>
 * <ul>
 * <li>Attributes: a key-value map containing information of the chapter</li>
 * <li>Sub-chapters: a list containing all sub-chapters, optional</li>
 * <li>Text: main text of the chapter, optional</li>
 * <li>Tag: extra data field used by user, optional</li>
 * <li>Cleanups: actions for cleanup resources of the chapter</li>
 * </ul>
 */
public class Chapter implements Hierarchial<Chapter>, Cloneable {
    private static final String TAG = "Chapter";

    /**
     * Attributes of the chapter.
     */
    @Getter
    private VariantMap attributes = Attributes.newAttributes();

    /**
     * Extra data field used by user.
     */
    @Getter
    @Setter
    private Object tag = null;

    /**
     * Optional text of the chapter.
     */
    @Getter
    @Setter
    private Text text = null;

    /**
     * Constructs instance without attributes.
     */
    public Chapter() {
    }

    /**
     * Constructs instance with specified title
     *
     * @param title the chapter title
     * @throws NullPointerException if the title is null
     */
    public Chapter(String title) {
        Attributes.set(this, Attributes.TITLE, title);
    }

    /**
     * Constructs instance with specified title and specified text.
     *
     * @param title the chapter title
     * @param text  main text
     * @throws NullPointerException if the title is null
     */
    public Chapter(String title, Text text) {
        Attributes.set(this, Attributes.TITLE, title);
        this.text = text;
    }

    /**
     * Constructs instance with coping data from specified chapter.
     *
     * @param chapter  the chapter to be copied
     * @param deepCopy {@code true} to clone all sub-chapters
     * @throws NullPointerException if the chapter is null
     */
    public Chapter(@NonNull Chapter chapter, boolean deepCopy) {
        dumpTo(chapter, deepCopy);
    }

    // *************************************** \\
    // **** Contents Hierarchy Operations **** \\
    // *************************************** \\

    @Getter
    private Chapter parent = null;

    private ArrayList<Chapter> children = new ArrayList<>();

    /**
     * Appends specified chapter to the end of sub-chapter list.
     *
     * @param chapter chapter to be appended to sub-chapter list
     * @throws NullPointerException     if the specified chapter is null
     * @throws IllegalArgumentException if the specified chapter is not solitary
     */
    public final void append(@NonNull Chapter chapter) {
        children.add(requireSolitary(chapter));
        chapter.parent = this;
    }

    /**
     * Inserts the specified chapter at the specified position in sub-chapter list.
     *
     * @param index   index at which the specified chapter is to be inserted
     * @param chapter chapter to be inserted
     * @throws NullPointerException      if the specified chapter is null
     * @throws IllegalArgumentException  if the specified chapter is not solitary
     * @throws IndexOutOfBoundsException if the index is out of range (<tt>index &lt; 0 || index &gt; size()</tt>)
     */
    public final void insert(int index, @NonNull Chapter chapter) {
        children.add(index, requireSolitary(chapter));
        chapter.parent = this;
    }

    /**
     * Returns the number of chapters in sub-chapter list.
     *
     * @return the number of chapters in sub-chapter list
     */
    public final int size() {
        return children.size();
    }

    /**
     * Returns <tt>true</tt> if sub-chapter list is not empty.
     *
     * @return <tt>true</tt> if sub-chapter list is not empty
     */
    public final boolean isSection() {
        return !children.isEmpty();
    }

    /**
     * Returns the index of the first occurrence of the specified chapter in sub-chapter list, or -1 if sub-chapter list
     * does not contain the chapter.
     *
     * @param chapter chapter to search for
     * @return the index of the first occurrence of the specified chapter in sub-chapter list, or -1 if sub-chapter list
     * does not contain the chapter
     */
    public final int indexOf(Chapter chapter) {
        return chapter == null || chapter.parent != this ? -1 : children.indexOf(chapter);
    }

    /**
     * Returns the chapter at the specified position in sub-chapter list.
     *
     * @param index index of the chapter to return
     * @return the chapter at the specified position in sub-chapter list
     * @throws IndexOutOfBoundsException if the index is out of range (<tt>index &lt; 0 || index &gt;= size()</tt>)
     */
    public final Chapter chapterAt(int index) {
        return children.get(index);
    }

    /**
     * Replaces the specified chapter in sub-chapter list with the specified new chapter.
     *
     * @param chapter the chapter to be replaced
     * @param target  chapter to be stored at the specified position
     * @return <tt>true</tt> if sub-chapter list contained the specified chapter
     * @throws NullPointerException     if the specified chapter is null
     * @throws IllegalArgumentException if the specified chapter is not solitary
     */
    public final boolean replace(@NonNull Chapter chapter, @NonNull Chapter target) {
        val index = indexOf(chapter);
        if (index == -1) {
            return false;
        }
        children.set(index, requireSolitary(target));
        chapter.parent = this;
        chapter.parent = null;
        return true;
    }

    /**
     * Replaces the chapter at the specified position in sub-chapter with the specified chapter.
     *
     * @param index   index of the chapter to replace
     * @param chapter chapter to be stored at the specified position
     * @return chapter previously at the specified position
     * @throws NullPointerException      if the specified chapter is null
     * @throws IllegalArgumentException  if the specified chapter is not solitary
     * @throws IndexOutOfBoundsException if the index is out of range (<tt>index &lt; 0 || index &gt; size()</tt>)
     */
    public final Chapter replaceAt(int index, @NonNull Chapter chapter) {
        val current = children.set(index, requireSolitary(chapter));
        chapter.parent = this;
        current.parent = null;
        return current;
    }

    /**
     * Removes the first occurrence of the specified chapter from sub-chapter list, if it is present.
     *
     * @param chapter chapter to be removed from this sub-chapter list, if present
     * @return <tt>true</tt> if sub-chapter list contained the specified chapter
     * @throws NullPointerException if the specified chapter is null
     */
    public final boolean remove(@NonNull Chapter chapter) {
        if (chapter.parent != this) {
            return false;
        }
        if (children.remove(chapter)) {
            chapter.parent = null;
            return true;
        }
        return false;
    }

    /**
     * Removes the chapter at the specified position in sub-chapter.
     *
     * @param index the index of the chapter to be removed
     * @return the chapter previously at the specified position
     */
    public final Chapter removeAt(int index) {
        val current = children.remove(index);
        current.parent = null;
        return current;
    }

    /**
     * Swaps the chapters at the specified positions in sub-chapter list.
     *
     * @param from the index of one chapter to be swapped
     * @param to   the index of the other chapter to be swapped
     * @throws IndexOutOfBoundsException if either <tt>from</tt> or <tt>to</tt> is out of range (from &lt; 0 || from
     *                                   &gt;= size() || to &lt; 0 || to &gt;= size()).
     */
    public final void swap(int from, int to) {
        Collections.swap(children, from, to);
    }

    /**
     * Removes all of the chapters from sub-chapter list.
     * <p>
     * Cleanups of chapters in sub-chapter list will be called.
     * </p>
     */
    public final void clear() {
        clear(true);
    }

    /**
     * Removes all of the chapters from sub-chapter list.
     *
     * @param cleanup <tt>true</tt> to call <tt>cleanup</tt> for all chapters
     */
    public final void clear(boolean cleanup) {
        for (val chapter : children) {
            chapter.parent = null;
            if (cleanup) {
                chapter.cleanup();
            }
        }
        children.clear();
    }

    @Override
    public final Iterator<Chapter> iterator() {
        return children.iterator();
    }

    private Chapter requireSolitary(Chapter chapter) {
        require(chapter != this, "Cannot add self to sub-chapter list: %s", chapter);
        require(chapter.parent == null, "Chapter has been in certain chapter: %s", chapter);
        require(chapter != parent, "Cannot add parent to sub-chapter list: %s", chapter);
        return chapter;
    }

    // ************************************** \\
    // **** Resources Cleanup Operations **** \\
    // ************************************** \\

    private boolean cleaned = false;

    private Set<Consumer<? super Chapter>> cleanups = new LinkedHashSet<>();

    /**
     * Registers specified cleanup action.
     *
     * @param cleanup the action to be executed when cleaning up the chapter
     */
    public final void registerCleanup(@NonNull Consumer<? super Chapter> cleanup) {
        cleanups.add(cleanup);
    }

    /**
     * Removes specified cleanup action from cleanup list.
     *
     * @param cleanup the cleanup to be removed
     */
    public final void removeCleanup(@NonNull Consumer<? super Chapter> cleanup) {
        cleanups.remove(cleanup);
    }

    /**
     * Cleans up this chapter.
     * <p>
     * Steps of cleanup:
     * </p>
     * <ul>
     * <li>Executes all register cleanup actions in registering order</li>
     * <li>Removes all attributes of this chapter</li>
     * <li>Cleans up chapters in sub-chapter list</li>
     * <li>Removes this chapter form its parent, if present</li>
     * </ul>
     */
    public void cleanup() {
        if (cleaned) {
            return;
        }
        for (val cleanup : cleanups) {
            cleanup.accept(this);
        }
        cleanups.clear();
        attributes.clear();
        clear(true);
        if (parent != null) {
            parent.remove(this);
        }
        cleaned = true;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (!cleaned) {
            Log.w(TAG, "chapter {0} is not cleaned", this);
        }
    }

    @Override
    @SneakyThrows(CloneNotSupportedException.class)
    public Chapter clone() {
        val copy = (Chapter) super.clone();
        copy.parent = null;
        dumpTo(copy, true);
        return copy;
    }

    protected void dumpTo(Chapter chapter, boolean deepCopy) {
        chapter.attributes = attributes.clone();
        if (deepCopy) {
            chapter.children = new ArrayList<>(children.size());
            for (val i : children) {
                chapter.append(i.clone());
            }
        }
    }

    @Override
    public String toString() {
        return String.format("Chapter{attributes=%s, tag=%s, text=%s}", attributes, text, tag);
    }
}
