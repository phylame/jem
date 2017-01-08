/*
 * Copyright 2014-2017 Peng Wan <phylame@163.com>
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

import lombok.Getter;
import pw.phylame.jem.util.VariantMap;

/**
 * Common <code>Book</code> model describes book structure.
 * <p>
 * A book structure contains the following parts:
 * </p>
 * <ul>
 * <li>attributes - meta attributes of book</li>
 * <li>contents - table of contents</li>
 * <li>extensions - extension data, like attributes but not part of book itself</li>
 * </ul>
 */
public class Book extends Chapter {
    /**
     * Constructs instance with empty title.
     */
    public Book() {
        super();
    }

    /**
     * Constructs instance with specified title.
     *
     * @param title the title of book
     */
    public Book(String title) {
        super(title);
    }

    /**
     * Constructs instance with specified title and author.
     *
     * @param title  the title string
     * @param author the author string
     * @throws NullPointerException if the argument list contains <code>null</code>
     */
    public Book(String title, String author) {
        super(title);
        Attributes.setAuthor(this, author);
    }

    public Book(Chapter chapter) {
        chapter.dumpTo(this);
    }

    // *********************
    // ** Extension items **
    // *********************

    /**
     * Extensions map.
     */
    @Getter
    private VariantMap extensions = new VariantMap();

    @Override
    public void cleanup() {
        extensions.clear();
        super.cleanup();
    }

    @Override
    protected void dumpTo(Chapter chapter) {
        super.dumpTo(chapter);
        if (chapter instanceof Book) {
            ((Book) chapter).extensions = extensions.clone();
        }
    }

    @Override
    public String debug() {
        return super.debug() + ", extensions: " + extensions;
    }
}
