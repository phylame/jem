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

import jclp.util.Validate;
import jem.Attributes;
import jem.Book;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

import java.io.File;
import java.util.Map;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class EpmOutParam extends EpmParam {
    /**
     * The book to write.
     */
    private final Book book;

    public EpmOutParam(Book book, @NonNull File file, Map<String, Object> arguments) {
        this(book, file, null, null, arguments);
    }

    public EpmOutParam(Book book, @NonNull String output, Map<String, Object> arguments) {
        this(book, null, output, null, arguments);
    }

    public EpmOutParam(Book book, @NonNull File file, String format, Map<String, Object> arguments) {
        this(book, file, null, format, arguments);
    }

    public EpmOutParam(Book book, @NonNull String output, String format, Map<String, Object> arguments) {
        this(book, null, output, format, arguments);
    }

    public EpmOutParam(@NonNull Book book, File file, String path, String format, Map<String, Object> arguments) {
        super(file, path, format, arguments);
        if (file == null) {
            Validate.requireNotEmpty(path, "path cannot be empty");
        }
        this.book = book;
    }

    public final File getOutput() {
        File file = getFile();
        if (file == null) {
            file = new File(getPath());
        }
        if (file.isDirectory()) {
            file = new File(file, Attributes.getTitle(book) + "." + getFormat());
        }
        return file;
    }
}
