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

package pw.phylame.jem.epm.base;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import lombok.NonNull;
import lombok.val;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.epm.Parser;
import pw.phylame.jem.epm.util.E;
import pw.phylame.jem.epm.util.InputCleaner;
import pw.phylame.jem.epm.util.ParserException;
import pw.phylame.jem.epm.util.config.EpmConfig;
import pw.phylame.jem.util.JemException;

public abstract class AbstractParser<I extends Closeable, C extends EpmConfig> extends BookWorker<C> implements Parser {

    protected AbstractParser(String name, Class<C> clazz) {
        super(name, clazz);
    }

    protected abstract I open(File file, C config) throws IOException;

    protected abstract Book parse(I input, C config) throws IOException, ParserException;

    @Override
    public synchronized final Book parse(@NonNull File file, Map<String, Object> args)
            throws IOException, JemException {
        if (!file.exists()) {
            throw E.forFileNotFound("No such file: %s", file.getPath());
        }
        val config = fetchConfig(args);
        val input = open(file, config);
        if (input == null) {
            throw E.forParser("'open(File, C)' of '%s' returned null", getClass().getName());
        }
        Book book;
        try {
            book = parse(input, config);
            if (book == null) {
                throw E.forParser("'parse(I, C)' of '%s' returned null", getClass().getName());
            }
        } catch (IOException | JemException | RuntimeException e) {
            input.close();
            throw e;
        }
        // close the input when book is in cleanup
        book.registerCleanup(new InputCleaner(input));
        return book;
    }
}
