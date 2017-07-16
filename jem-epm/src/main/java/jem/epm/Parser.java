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

package jem.epm;

import jem.Book;
import jem.util.JemException;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * {@code Parser} used for parsing book file and stores to {@code Book}.
 * <p><strong>NOTE: </strong> the instance of {@code Parser} must be reusable.
 */
public interface Parser {
    /**
     * Key for storing meta data of book in extensions.
     * <p></p><strong>NOTE: </strong>the corresponding item should be ignored by {@code Maker}.
     */
    String META_KEY = "jem.ext.meta";

    /**
     * Returns the format name (normally be the extension name).
     *
     * @return the name of format for this parser
     */
    String getName();

    /**
     * Parses book file and stores to {@code Book}.
     *
     * @param file the input book file
     * @param args arguments to the parser
     * @return {@code Book} from the input file
     * @throws IOException  if occurs I/O errors
     * @throws JemException if occurs errors when parsing book file
     */
    Book parse(File file, Map<String, Object> args) throws IOException, JemException;

    /**
     * Parses book from specified input path.
     *
     * @param input path to input
     * @param args  arguments to the parser
     * @return {@code } from the input path
     * @throws IOException  if occurs I/O errors
     * @throws JemException if occurs errors when parsing book file
     * @since 3.2.0
     */
    Book parse(String input, Map<String, Object> args) throws IOException, JemException;
}
