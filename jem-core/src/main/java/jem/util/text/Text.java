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

package jem.util.text;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

/**
 * An object providing large unicode text.
 */
public interface Text extends Iterable<String> {
    /**
     * Returns the content type of content.
     *
     * @return the content type of content
     */
    String getType();

    /**
     * Returns the content of this object.
     *
     * @return the content of this object
     */
    String toString();

    /**
     * Returns an iterator over lines of content.
     *
     * @return an iterator
     */
    Iterator<String> iterator();

    /**
     * Writes content of this object to specified writer.
     *
     * @param writer the writer to be written to
     * @return number of written characters
     * @throws NullPointerException if specified output is null
     * @throws IOException          if an I/O error occurs
     */
    long writeTo(Writer writer) throws IOException;
}
