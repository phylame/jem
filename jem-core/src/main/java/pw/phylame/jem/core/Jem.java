/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
 *
 * This file is part of Jem.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pw.phylame.jem.core;

import java.util.List;

import lombok.NonNull;
import pw.phylame.ycl.util.Function;
import pw.phylame.ycl.util.MiscUtils;

/**
 * This class contains utility methods for book operations.
 */
public final class Jem {
    private Jem() {
    }

    /**
     * Finds child chapter with specified index from chapter sub-chapter tree.
     *
     * @param chapter
     *            the <code>Chapter</code> to be indexed
     * @param indices
     *            list of index in sub-chapter tree
     * @return the <code>Chapter</code>, never <code>null</code>
     * @throws NullPointerException
     *             if the chapter or indices is <code>null</code>
     * @throws IndexOutOfBoundsException
     *             if the index in indices is out of range (index &lt; 0 || index &ge; size())
     */
    public static Chapter locate(@NonNull Chapter chapter, @NonNull int[] indices) {
        return MiscUtils.locate(chapter, indices);
    }

    /**
     * Returns the depth of sub-chapter tree in specified chapter.
     *
     * @param chapter
     *            the chapter
     * @return depth of the chapter
     * @throws NullPointerException
     *             if the chapter is <code>null</code>
     */
    public static int depthOf(@NonNull Chapter chapter) {
        return MiscUtils.depthOf(chapter);
    }

    /**
     * Finds a matched sub-chapter from specified chapter with filter.
     *
     * @param chapter
     *            the parent chapter
     * @param filter
     *            the filter
     * @param from
     *            begin index of sub-chapter to be filtered in <code>chapter</code>
     * @param recursion
     *            <code>true</code> to find sub-chapter(s) of <code>chapter</code>
     * @return the first matched chapter or <code>null</code> if no matched found
     */
    public static Chapter find(@NonNull Chapter chapter, @NonNull Function<Chapter, Boolean> filter, int from,
            boolean recursion) {
        return MiscUtils.find(chapter, filter, from, recursion);
    }

    /**
     * Selected sub-chapter from specified chapter with specified condition.
     *
     * @param chapter
     *            the parent chapter
     * @param filter
     *            the filter
     * @param result
     *            store matched chapters
     * @param limit
     *            limits of matched chapters
     * @param recursion
     *            <code>true</code> to find sub-chapter(s) of <code>chapter</code>
     * @return the number of found chapter(s)
     */
    public static int select(@NonNull Chapter chapter, @NonNull Function<Chapter, Boolean> filter,
            @NonNull List<Chapter> result, int limit, boolean recursion) {
        return MiscUtils.select(chapter, filter, result, limit, recursion);
    }
}
