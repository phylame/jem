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

package pw.phylame.jem.util.text;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Provides reused and read-only unicode text source.
 */
public interface Text extends Iterable<String> {
    /**
     * Returns the type of content.
     *
     * @return the type string
     */
    String getType();

    /**
     * Returns text of this object.
     *
     * @return the string of text, never be <code>null</code>
     */
    String getText();

    /**
     * Returns list of string of text split by line separator.
     *
     * @param skipEmpty
     *            <code>true</code> to skip empty lines
     * @return list of string, may be empty list
     */
    List<String> getLines(boolean skipEmpty);

    /**
     * Writes text of this object to output writer.
     *
     * @param writer
     *            output to store text text
     * @return number of written characters
     * @throws NullPointerException
     *             if the writer is {@literal null}
     * @throws IOException
     *             if occur I/O errors
     */
    long writeTo(Writer writer) throws IOException;
}
