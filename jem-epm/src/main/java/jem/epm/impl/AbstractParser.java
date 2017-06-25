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

package jem.epm.impl;

import jem.Book;
import jem.epm.Parser;
import jem.epm.util.E;
import jem.epm.util.InputCleaner;
import jem.epm.util.ParserException;
import jem.epm.util.config.EpmConfig;
import jem.util.JemException;
import lombok.NonNull;
import lombok.val;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public abstract class AbstractParser<I extends Closeable, C extends EpmConfig> extends EpmBase<C> implements Parser {

    protected AbstractParser(String name, Class<C> clazz) {
        super(name, clazz);
    }

    protected abstract I openInput(File file, C config) throws IOException;

    public abstract Book parse(I input, C config) throws IOException, ParserException;

    @Override
    public Book parse(@NonNull File file, Map<String, Object> args) throws IOException, JemException {
        if (!file.exists()) {
            throw E.forFileNotFound("No such file: %s", file.getPath());
        }
        val config = fetchConfig(args);
        val input = openInput(file, config);
        if (input == null) {
            throw E.forParser("'open(File, C)' of '%s' returned null", getClass().getName());
        }
        val cleaner = new InputCleaner(input);
        final Book book;
        try {
            book = parse(input, config);
            if (book == null) {
                throw E.forParser("'parse(I, C)' of '%s' returned null", getClass().getName());
            }
        } catch (IOException | JemException | RuntimeException e) {
            cleaner.accept(null);
            throw e;
        }
        // close the input when book is in cleanup
        book.registerCleanup(cleaner);
        return book;
    }

    @Override
    public Book parse(String input, Map<String, Object> args) throws IOException, JemException {
        return parse(new File(input), args);
    }
}
