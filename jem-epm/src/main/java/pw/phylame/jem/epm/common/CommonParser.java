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

package pw.phylame.jem.epm.common;

import lombok.NonNull;
import lombok.val;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.epm.Parser;
import pw.phylame.jem.epm.util.ParserException;
import pw.phylame.jem.epm.util.SourceCleaner;
import pw.phylame.jem.epm.util.config.Config;
import pw.phylame.jem.util.JemException;
import pw.phylame.ycl.util.Exceptions;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public abstract class CommonParser<I extends Closeable, C extends Config> extends BookWorker<C> implements Parser {
    /**
     * The input file to parse.
     * <p>This value will be accessible after {@link #validateFile(Closeable, CommonConfig)}
     */
    protected File source;

    protected CommonParser(@NonNull String name, String cfgkey, Class<C> cfgcls) {
        super(name, cfgkey, cfgcls);
    }

    protected abstract I openInput(File file, C config) throws IOException;

    protected abstract Book parse(I input, C config) throws IOException, ParserException;

    @Override
    public final Book parse(@NonNull File file, Map<String, Object> args) throws IOException, JemException {
        if (!file.exists()) {
            throw Exceptions.forFileNotFound("No such file: %s", file.getPath());
        }
        val config = fetchConfig(args);
        val input = openInput(file, config);
        Book book;
        try {
            source = file;
            book = parse(input, config);
        } catch (IOException | JemException | RuntimeException e) {
            input.close();
            throw e;
        }
        book.registerCleanup(new SourceCleaner(input));
        return book;
    }
}
