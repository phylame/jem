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

package jem.epm.impl;

import jclp.io.IOUtils;
import jclp.setting.Settings;
import jem.Book;
import jem.epm.util.InputCleaner;
import jem.util.JemException;
import lombok.val;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static jclp.util.Validate.checkNotNull;

public abstract class AbstractParser<IN extends Closeable> implements FileParser {
    protected abstract IN open(File file, Settings arguments) throws IOException;

    protected abstract Book parse(IN input, Settings arguments) throws IOException, JemException;

    public Book parse(File file, Settings arguments) throws IOException, JemException {
        if (!file.exists()) {
            throw new FileNotFoundException("No such file or directory: " + file);
        }
        val input = open(file, arguments);
        checkNotNull(input, "openFile of %s returned null", this);
        Book book;
        try {
            book = parse(input, arguments);
            checkNotNull(book, "parse of %s returned null", this);
        } catch (Exception e) {
            IOUtils.closeQuietly(input);
            throw e;
        }
        book.registerCleanup(new InputCleaner(input));
        return book;
    }

    @Override
    public Book parse(String input, Settings arguments) throws IOException, JemException {
        return parse(new File(input), arguments);
    }

    @SuppressWarnings("unchecked")
    protected <T> T get(Settings arguments, String key) {
        return (T) arguments.get("parser." + key);
    }
}
