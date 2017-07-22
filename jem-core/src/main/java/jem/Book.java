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

import jem.util.VariantMap;
import lombok.Getter;
import lombok.NonNull;

/**
 * The book in Jem book models.
 */
public class Book extends Chapter {
    /**
     * Constructs instance without attributes.
     */
    public Book() {
    }

    /**
     * Constructs instance with specified book title.
     *
     * @param title the book title
     * @throws NullPointerException if the title is null
     */
    public Book(String title) {
        super(title);
    }

    /**
     * Constructs instance with coping data form specified chapter.
     *
     * @param chapter  the chapter to be copied
     * @param deepCopy {@code true} to clone all sub-chapters
     * @throws NullPointerException if the chapter is null
     */
    public Book(@NonNull Chapter chapter, boolean deepCopy) {
        chapter.dumpTo(chapter, deepCopy);
    }

    /**
     * Extensions of the book.
     */
    @Getter
    private VariantMap extensions = new VariantMap();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void dumpTo(Chapter chapter, boolean deepCopy) {
        super.dumpTo(chapter, deepCopy);
        if (chapter instanceof Book) {
            ((Book) chapter).extensions = extensions.clone();
        }
    }

    @Override
    public String toString() {
        return super.toString() + ",extension=" + extensions;
    }
}
