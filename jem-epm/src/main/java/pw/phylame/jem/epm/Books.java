/*
 * Copyright 2016 Peng Wan <phylame@163.com>
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

package pw.phylame.jem.epm;

import lombok.NonNull;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.util.JemException;
import pw.phylame.jem.util.UnsupportedFormatException;
import pw.phylame.ycl.io.IOUtils;
import pw.phylame.ycl.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public final class Books {
    private Books() {
    }

    private static final String TAG = "BOOKS";

    /**
     * The default format of Jem.
     */
    public static final String PMAB = "pmab";

    /**
     * Gets the format of specified file path.
     *
     * @param path the path string
     * @return string represent the format
     */
    public static String formatOfExtension(String path) {
        return EpmManager.nameOfExtension(IOUtils.getExtension(path).toLowerCase());
    }

    public static Parser parserForFormat(@NonNull String format) throws UnsupportedFormatException {
        Parser parser = null;
        try {
            parser = EpmManager.parserFor(format);
        } catch (IllegalAccessException | ClassNotFoundException | InstantiationException e) {
            Log.e(TAG, e);
        }
        if (parser == null) {
            throw new UnsupportedFormatException(format, "Unsupported format '" + format + '\'');
        }
        return parser;
    }

    /**
     * Reads <code>Book</code> from book file.
     *
     * @param input     book file to be read
     * @param format    format of the book file
     * @param arguments arguments to parser
     * @return <code>Book</code> instance represents the book file
     * @throws NullPointerException if the file or format is <code>null</code>
     * @throws IOException          if occurs I/O errors
     * @throws JemException         if occurs errors when parsing book file
     */
    public static Book readBook(@NonNull File input, String format, Map<String, Object> arguments)
            throws IOException, JemException {
        return parserForFormat(format).parse(input, arguments);
    }

    public static Maker makerForFormat(@NonNull String format) throws UnsupportedFormatException {
        Maker maker = null;
        try {
            maker = EpmManager.makerFor(format);
        } catch (IllegalAccessException | ClassNotFoundException | InstantiationException e) {
            Log.e(TAG, e);
        }
        if (maker == null) {
            throw new UnsupportedFormatException(format, "Unsupported format '" + format + "'");
        }
        return maker;
    }

    /**
     * Writes <code>Book</code> to book with specified format.
     *
     * @param book      the <code>Book</code> to be written
     * @param output    output book file
     * @param format    output format
     * @param arguments arguments to maker
     * @throws NullPointerException if the book, output or format is <code>null</code>
     * @throws IOException          if occurs I/O errors
     * @throws JemException         if occurs errors when making book file
     */
    public static void writeBook(@NonNull Book book, @NonNull File output, String format, Map<String, Object> arguments)
            throws IOException, JemException {
        makerForFormat(format).make(book, output, arguments);
    }
}
