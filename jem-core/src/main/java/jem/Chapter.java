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
import jclp.util.Hierarchical;
import jem.util.VariantMap;
import jem.util.flob.Flob;
import jem.util.text.Text;
import lombok.*;

import java.lang.ref.WeakReference;
import java.util.*;

import static jclp.util.Validate.require;
import static jem.Attributes.*;

/**
 * <p>
 * Common chapter model in book contents.
 * </p>
 * <p>
 * The <code>Chapter</code> represents base element of book, it may be:
 * </p>
 * <ul>
 * <li>Chapter: common chapter</li>
 * <li>Section: collection of chapters</li>
 * <li>and others</li>
 * </ul>
 * <p>
 * A common <code>Chapter</code> structure contains following parts:
 * </p>
 * <ul>
 * <li>attributes map: a string-value map contains information of chapter</li>
 * <li>text text: main text of the chapter, provided by {@code Text} text</li>
 * <li>sub-chapter list: list of sub chapters</li>
 * <li>clean works: task for cleaning resources and others</li>
 * </ul>
 */
public class Chapter implements Hierarchical<Chapter>, Cloneable {
    /**
     * Constructs chapter with empty title.
     */
    public Chapter() {
        setTitle(this, "");
    }

    /**
     * Constructs chapter with specified title.
     *
     * @param title title of chapter
     * @throws NullPointerException if the <code>title</code> is <code>null</code>
     */
    public Chapter(String title) {
        setTitle(this, title);
    }

    /**
     * Constructs chapter with specified title and text.
     *
     * @param title title of chapter
     * @param text  text text provider
     * @throws NullPointerException if the title or text is {@literal null}
     */
    public Chapter(String title, Text text) {
        setTitle(this, title);
        setText(text);
    }

    /**
     * Constructs chapter with specified title, text, cover image and intro text.
     *
     * @param title title of chapter
     * @param cover <code>Flob</code> contains cover image, <code>null</code> will be ignored
     * @param intro intro text, <code>null</code> will be ignored
     * @param text  text text provider
     * @throws NullPointerException if the argument list contains <code>null</code>
     */
    public Chapter(String title, Flob cover, Text intro, Text text) {
        setTitle(this, title);
        setCover(this, cover);
        setIntro(this, intro);
        setText(text);
    }

    public Chapter(@NonNull Chapter chapter) {
        chapter.dumpTo(this);
    }

    /**
     * Attributes of the chapter.
     */
    @Getter
    private VariantMap attributes = new VariantMap(new HashMap<String, Object>(), variantValidator);

    /**
     * Content of the chapter.
     */
    @Getter
    @Setter
    private Text text;

    /**
     * Extra data field used by user.
     */
    @Getter
    @Setter
    private Object tag;

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
    @Override
    public final Chapter getParent() {
        return parent != null ? parent.get() : null;
    }

    private void setParent(Chapter parent) {
        this.parent = new WeakReference<>(parent);
    }

    /**
     * Sub-chapters list.
     */
    private List<Chapter> chapters = new ArrayList<>();

    private Chapter checkChapter(@NonNull Chapter chapter) {
        require(chapter != this, "Cannot add self to sub chapter list: %s", chapter);
        require(chapter.getParent() == null, "Chapter already in a certain section: %s", chapter);
        require(chapter != getParent(), "Cannot add parent chapter to its sub chapter list: %s", chapter);
        return chapter;
    }

    /**
     * Appends the specified chapter to the end of sub-chapter list.
     *
     * @param chapter the <code>Chapter</code> to be added
     * @throws NullPointerException if the <code>chapter</code> is <code>null</code>
     */
    public final void append(Chapter chapter) {
        chapters.add(checkChapter(chapter));
        chapter.setParent(this);
    }

    /**
     * Inserts the specified chapter at specified position in sub-chapter list.
     *
     * @param index   index of the chapter to be inserted
     * @param chapter the <code>Chapter</code> to be added
     * @throws NullPointerException      if the <code>chapter</code> is <code>null</code>
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &ge; size())
     */
    public final void insert(int index, Chapter chapter) {
        chapters.add(index, checkChapter(chapter));
        chapter.setParent(this);
    }

    /**
     * Returns the index of the first occurrence of the specified chapter in sub chapters list.
     *
     * @param chapter the chapter to search of
     * @return the index or <code>-1</code> if specified chapter not presents
     * @throws NullPointerException if the <code>chapter</code> is <code>null</code>
     */
    public final int indexOf(@NonNull Chapter chapter) {
        return chapter.getParent() != this ? -1 : chapters.indexOf(chapter);
    }

    /**
     * Removes the chapter at specified position from sub-chapter list.
     *
     * @param index index of the chapter to be removed
     * @return the chapter at specified position or <code>null</code> if <code>index</code> not exists
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &ge; size())
     */
    public final Chapter removeAt(int index) {
        val chapter = chapters.remove(index);
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
        if (chapter.getParent() != this) { // to be faster
            return false;
        }
        if (chapters.remove(chapter)) { // contained in list
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
        val previous = chapters.set(index, checkChapter(chapter));
        chapter.setParent(this);
        previous.parent = null;
        return previous;
    }

    /**
     * Returns the chapter at specified position in sub-chapter list.
     *
     * @param index index of the chapter to return
     * @return the chapter at specified position or <code>null</code> if <code>index</code> not exists
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &ge; size())
     */
    public final Chapter chapterAt(int index) {
        return chapters.get(index);
    }

    /**
     * Swaps position of two specified chapters.
     *
     * @param from position of the chapter
     * @param to   position of another chapter
     */
    public final void swap(int from, int to) {
        Collections.swap(chapters, from, to);
    }

    public final void clear() {
        clear(true);
    }

    /**
     * Removes all chapters from sub-chapter list.
     */
    public final void clear(boolean cleanup) {
        for (val chapter : chapters) {
            if (cleanup) {
                chapter.cleanup();
            }
            chapter.parent = null;
        }
        chapters.clear();
    }

    /**
     * Returns size of sub-chapter list.
     *
     * @return number of sub-chapters
     */
    @Override
    public final int size() {
        return chapters.size();
    }

    @Override
    public List<Chapter> getChildren() {
        return Collections.unmodifiableList(chapters);
    }

    /**
     * Tests this object is a section or not.
     * <p>
     * A section without text text is a container of chapters.
     * </p>
     *
     * @return <code>true</code> if has sub-chapters otherwise <code>false</code>
     */
    public final boolean isSection() {
        return !chapters.isEmpty();
    }

    /**
     * Returns an iterator over sub-chapter list.
     *
     * @return an Iterator for chapter.
     */
    @Override
    public final Iterator<Chapter> iterator() {
        return chapters.iterator();
    }

    // *****************
    // ** Clean works **
    // *****************

    /**
     * Clean works
     */
    private final Set<Consumer<? super Chapter>> cleaners = new LinkedHashSet<>();

    /**
     * Registers the specified clean task to clean works list.
     *
     * @param task the clean task instance
     * @throws NullPointerException if the specified clean task is <code>null</code>
     */
    public void registerCleanup(@NonNull Consumer<? super Chapter> task) {
        cleaners.add(task);
    }

    /**
     * Removes the specified clean task from clean works list.
     *
     * @param task the clean task to be removed, if <code>null</code> do nothing
     */
    public void removeCleanup(Consumer<? super Chapter> task) {
        if (task != null) {
            cleaners.remove(task);
        }
    }

    /**
     * Cleans up the chapter.
     * <p>
     * Tasks to process:
     * <ul>
     * <li>Invoke all registered clean task(the task list will also be cleared)</li>
     * <li>Remove all attributes.</li>
     * <li>Do clean up of all sub-chapter.(the sub-chapter list will also be cleared)</li>
     * </ul>
     */
    public void cleanup() {
        for (val work : cleaners) {
            work.accept(this);
        }
        cleaners.clear();
        // remove all attributes
        attributes.clear();

        // clean all sub chapters
        clear(true);

        cleaned = true;
    }

    private boolean cleaned = false;

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (!cleaned) {
            Log.e("Book", "*** BUG: Chapter \"{0}@{1}\" not cleaned ***\n", getTitle(this), hashCode());
        }
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows({InstantiationException.class, IllegalAccessException.class})
    protected void dumpTo(Chapter chapter) {
        chapter.text = text;
        chapter.attributes = attributes.clone();
        chapter.chapters = chapters.getClass().newInstance();
        chapter.chapters.addAll(chapters);
    }

    /**
     * Returns a shallow copy of this <code>Chapter</code> instance.
     *
     * @return a shallow copy of this chapter
     */
    @Override
    public Chapter clone() {
        final Chapter copy;
        try {
            copy = (Chapter) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
        dumpTo(copy);
        return copy;
    }

    @Override
    public String toString() {
        return debug();
    }

    /**
     * Renders debug string of this chapter
     *
     * @return the string
     */
    public String debug() {
        if (text == null) {
            return String.format("%s@%d: attributes: %s", getClass().getSimpleName(), hashCode(), attributes);
        }
        return String.format("%s@%d: attributes: %s, text: %s", getClass().getSimpleName(), hashCode(), attributes, text);
    }
}
