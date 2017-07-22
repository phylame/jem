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
import jem.epm.impl.FileMaker;
import jem.epm.impl.FileParser;
import jem.epm.util.MakerParam;
import jem.epm.util.ParserParam;
import jem.epm.util.ServiceManager;
import jem.epm.util.UnsupportedFormatException;
import jem.util.JemException;
import lombok.NonNull;
import lombok.val;

import java.io.IOException;
import java.util.*;

public class EpmManager extends ServiceManager<EpmFactory> {
    private Map<String, EpmFactory> nameRegistry;

    public EpmManager() {
        super(EpmFactory.class);
    }

    public EpmManager(ClassLoader loader) {
        super(EpmFactory.class, loader);
    }

    @Override
    protected void init() {
        super.init();
        nameRegistry = new HashMap<>();
    }

    public Set<EpmFactory> getFactories() {
        val factories = new HashSet<EpmFactory>();
        factories.addAll(getServices());
        factories.addAll(nameRegistry.values());
        return Collections.unmodifiableSet(factories);
    }

    public EpmFactory getFactory(@NonNull String name) {
        val factory = nameRegistry.get(name);
        if (factory != null) {
            return factory;
        }
        for (val spi : getServices()) {
            val names = spi.getNames();
            if (names != null && names.contains(name)) {
                return spi;
            }
        }
        return null;
    }

    public void registerFactory(@NonNull String name, EpmFactory factory) {
        nameRegistry.put(name, factory);
    }

    public Parser getParser(@NonNull String name) {
        val factory = getFactory(name);
        return factory != null ? factory.getParser() : null;
    }

    public Maker getMaker(@NonNull String name) {
        val factory = getFactory(name);
        return factory != null ? factory.getMaker() : null;
    }

    public Book readBook(@NonNull ParserParam param) throws IOException, JemException {
        val parser = getParser(param.getFormat());
        if (parser == null) {
            throw new UnsupportedFormatException(param.getFormat());
        }
        val file = param.getFile();
        return file != null && parser instanceof FileParser
                ? ((FileParser) parser).parse(file, param.getArguments())
                : parser.parse(param.getInput(), param.getArguments());
    }

    public void writeBook(@NonNull MakerParam param) throws IOException, JemException {
        val maker = getMaker(param.getFormat());
        if (maker == null) {
            throw new UnsupportedFormatException(param.getFormat());
        }
        val file = param.getFile();
        if (file != null && maker instanceof FileMaker) {
            ((FileMaker) maker).make(param.getBook(), file, param.getArguments());
        } else {
            maker.make(param.getBook(), param.getOutput(), param.getArguments());
        }
    }
}
