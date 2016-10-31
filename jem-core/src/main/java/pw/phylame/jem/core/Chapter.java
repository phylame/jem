/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
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

package pw.phylame.jem.core;

import lombok.*;
import pw.phylame.jem.util.VariantMap;
import pw.phylame.jem.util.flob.Flob;
import pw.phylame.jem.util.text.Text;
import pw.phylame.ycl.util.StringUtils;
import pw.phylame.ycl.util.Validate;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>Common chapter model in book contents.</p>
 * <p>The <code>Chapter</code> represents base element of book, it may be:</p>
 * <ul>
 * <li>Chapter: common chapter</li>
 * <li>Section: collection of chapters</li>
 * <li>and others</li>
 * </ul>
 * <p>A common <code>Chapter</code> structure contains following parts:</p>
 * <ul>
 * <li>attributes map: a string-value map contains information of chapter</li>
 * <li>text text: main text of the chapter, provided by
 * <code>Text</code> text</li>
 * <li>sub-chapter list: list of sub chapters</li>
 * <li>clean works: task for cleaning resources and others</li>
 * </ul>
 */
public class Chapter implements Iterable<Chapter>, Cloneable {
    /**
     * Constructs chapter with empty title.
     */
    public Chapter() {
        Attributes.setTitle(this, StringUtils.EMPTY_TEXT);
    }

    /**
     * Constructs chapter with specified title.
     *
     * @param title title of chapter
     * @throws NullPointerException if the <code>title</code> is <code>null</code>
     */
    public Chapter(String title) {
        Attributes.setTitle(this, title);
    }

    /**
     * Constructs chapter with specified title and text.
     *
     * @param title title of chapter
     * @param text  text text provider
     * @throws NullPointerException if the argument list contains <code>null</code>
     */
    public Chapter(String title, Text text) {
        Attributes.setTitle(this, title);
        setText(text);
    }

    /**
     * Constructs chapter with specified title, text, cover image
     * and intro text.
     *
     * @param title title of chapter
     * @param cover <code>Flob</code> contains cover image, <code>null</code> will be ignored
     * @param intro intro text, <code>null</code> will be ignored
     * @param text  text text provider
     * @throws NullPointerException if the argument list contains <code>null</code>
     */
    public Chapter(String title, Flob cover, Text intro, Text text) {
        Attributes.setTitle(this, title);
        Attributes.setCover(this, cover);
        Attributes.setIntro(this, intro);
        setText(text);
    }

    public Chapter(@NonNull Chapter chapter) {
        chapter.dumpTo(this);
    }

    /**
     * Attributes of the chapter.
     */
    @Getter
    private VariantMap attributes = new VariantMap();

    /**
     * Content of the chapter.
     */
    @Getter
    @Setter
    @NonNull
    private Text text;

    // ****************************
    // ** Sub-chapter operations **
    // ****************************

    /**
     * Parent of current chapter.
     */
    private WeakReference<Chapter> parent = null;

    /**
     * Returns parent chapter of current chapter.
     *
     * @return the parent or <code>null</code> if not present
     */
    public final Chapter getParent() {
        return parent != null ? parent.get() : null;
    }

    private void setParent(Chapter parent) {
        this.parent = new WeakReference<>(parent);
    }

    /**
     * Sub-chapters list.
     */
    protected List<Chapter> children = new ArrayList<>();

    private Chapter checkChapter(@NonNull Chapter chapter) {
        Validate.require(chapter.getParent() == null, "Chapter already in a certain section: " + chapter);
        Validate.require(chapter != this, "Cannot add self to sub chapter list " + chapter);
        Validate.require(chapter != getParent(), "Cannot add parent chapter to its sub chapter list: " + chapter);
        return chapter;
    }

    /**
     * Appends the specified chapter to the end of sub-chapter list.
     *
     * @param chapter the <code>Chapter</code> to be added
     * @throws NullPointerException if the <code>chapter</code> is <code>null</code>
     */
    public final void append(Chapter chapter) {
        children.add(checkChapter(chapter));
        chapter.setParent(this);
    }

    /**
     * Inserts the specified chapter at specified position in sub-chapter list.
     *
     * @param index   index of the chapter to be inserted
     * @param chapter the <code>Chapter</code> to be added
     * @throws NullPointerException      if the <code>chapter</code> is <code>null</code>
     * @throws IndexOutOfBoundsException if the index is out of
     *                                   range (index &lt; 0 || index &ge; size())
     */
    public final void insert(int index, Chapter chapter) {
        children.add(index, checkChapter(chapter));
        chapter.setParent(this);
    }

    /**
     * Returns the index of the first occurrence of the specified chapter in
     * sub chapters list.
     *
     * @param chapter the chapter to search of
     * @return the index or <code>-1</code> if specified chapter not presents
     * @throws NullPointerException if the <code>chapter</code> is <code>null</code>
     */
    public final int indexOf(@NonNull Chapter chapter) {
        return chapter.getParent() != this ? -1 : children.indexOf(chapter);
    }

    /**
     * Removes the chapter at specified position from sub-chapter list.
     *
     * @param index index of the chapter to be removed
     * @return the chapter at specified position or <code>null</code>
     * if <code>index</code> not exists
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &ge; size())
     */
    public final Chapter removeAt(int index) {
        val chapter = children.remove(index);
        chapter.parent = null;
        return chapter;
    }

    /**
     * Removes the specified chapter from sub-chapter list.
     *
     * @param chapter chapter to be removed from sub-chapter list, if present
     * @return <code>true</code> if sub-chapter list contained the specified chapter
     * @throws NullPointerException if the <code>chapter</code> is <code>null</code>
     */
    public final boolean remove(@NonNull Chapter chapter) {
        // not contained in children list
        if (chapter.getParent() != this) {  // to be faster
            return false;
        }
        if (children.remove(chapter)) { // contained in list
            chapter.parent = null;
            return true;
        }
        return false;
    }

    /**
     * Replaces the chapter at specified position in sub-chapter list with specified chapter.
     *
     * @param index   index of the chapter to replace
     * @param chapter chapter to be stored at the specified position
     * @return the chapter previously at the specified position
     * @throws NullPointerException      if the <code>chapter</code> is <code>null</code>
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &ge; size())
     */
    public final Chapter replace(int index, Chapter chapter) {
        val previous = children.set(index, checkChapter(chapter));
        chapter.setParent(this);
        previous.parent = null;
        return previous;
    }

    /**
     * Returns the chapter at specified position in sub-chapter list.
     *
     * @param index index of the chapter to return
     * @return the chapter at specified position or <code>null</code>
     * if <code>index</code> not exists
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &ge; size())
     */
    public final Chapter chapterAt(int index) {
        return children.get(index);
    }

    /**
     * Removes all chapters from sub-chapter list.
     */
    public final void clear() {
        for (val chapter : children) {
            chapter.parent = null;
        }
        children.clear();
    }

    /**
     * Returns size of sub-chapter list.
     *
     * @return number of sub-chapters
     */
    public final int size() {
        return children.size();
    }

    /**
     * Tests this object is a section or not.
     * <p>A section without text text is a container of chapters.</p>
     *
     * @return <code>true</code> if has sub-chapters otherwise <code>false</code>
     */
    public final boolean isSection() {
        return !children.isEmpty();
    }

    /**
     * Returns an iterator over sub-chapter list.
     *
     * @return an Iterator for chapter.
     */
    @Override
    public final Iterator<Chapter> iterator() {
        return children.iterator();
    }

    // *****************
    // ** Clean works **
    // *****************

    /**
     * Clean works
     */
    private final List<Cleanable> cleaners = new LinkedList<>();

    /**
     * Registers the specified <code>Cleanable</code> to clean works list.
     *
     * @param clean the <code>Cleanable</code> instance, if <code>null</code> do nothing
     * @throws NullPointerException if the specified <code>clean</code> is <code>null</code>
     */
    public void registerCleanup(@NonNull Cleanable clean) {
        cleaners.add(clean);
    }

    /**
     * Removes the specified <code>Cleanable</code> from clean works list.
     *
     * @param clean the <code>Cleanable</code> to be removed, if <code>null</code> do nothing
     */
    public void removeCleanup(Cleanable clean) {
        if (clean != null) {
            cleaners.remove(clean);
        }
    }

    /**
     * Invokes all <code>Cleanable</code> in clean works list and in sub-chapter list.
     * <p>Clean works in sub-chapter list were invoked firstly then invokes this.</p>
     * <p>The clean works list after executing all cleanup works will be cleared.</p>
     * <p>After invoking this method, this <code>Chapter</code> should be invalid because
     * sub-chapter list and attribute map will also be cleared.</p>
     */
    public void cleanup() {
        for (val work : cleaners) {
            work.clean(this);
        }
        cleaners.clear();
        // remove all attributes
        attributes.clear();

        // clean all sub chapters
        for (val sub : children) {
            sub.cleanup();
            sub.parent = null;
        }
        children.clear();

        cleaned = true;
    }

    private boolean cleaned = false;

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (!cleaned) {
            System.err.printf("*** BUG: Chapter \"%s@%d\" not cleaned ***\n", Attributes.getTitle(this), hashCode());
        }
    }

    protected void dumpTo(Chapter chapter) {
        chapter.attributes = attributes.clone();
        chapter.children = new ArrayList<>(children);
        chapter.text = text;
    }

    /**
     * Returns a shallow copy of this <code>Chapter</code> instance.
     *
     * @return a shallow copy of this chapter
     */
    @Override
    @SneakyThrows(CloneNotSupportedException.class)
    public Chapter clone() {
        val result = (Chapter) super.clone();
        dumpTo(result);
        return result;
    }

    /**
     * Renders debug string of this chapter
     *
     * @return the string
     */
    public String debug() {
        if (text != null) {
            return String.format("%s@%d: attributes@%d:%s, text@%d: %s", getClass().getSimpleName(), hashCode(),
                    attributes.hashCode(), attributes, text.hashCode(), text);
        } else {
            return String.format("%s@%d: attributes@%d:%s", getClass().getSimpleName(), hashCode(),
                    attributes.hashCode(), attributes);
        }
    }

    @Override
    public String toString() {
        return Attributes.getTitle(this);
    }
}
