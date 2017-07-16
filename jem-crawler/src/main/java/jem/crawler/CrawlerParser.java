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

package jem.crawler;

import jclp.util.Validate;
import jem.Book;
import jem.epm.Parser;
import jem.epm.impl.EpmBase;
import jem.util.JemException;
import lombok.val;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class CrawlerParser extends EpmBase<CrawlerConfig> implements Parser {
    private static final String NAME = "crawler";

    public CrawlerParser() {
        super(NAME, CrawlerConfig.class);
    }

    @Override
    public Book parse(File file, Map<String, Object> args) throws IOException, JemException {
        throw new UnsupportedOperationException("Crawler parser is not supported for file");
    }

    @Override
    public Book parse(String input, Map<String, Object> args) throws IOException, JemException {
        val config = fetchConfig(args);
        Validate.requireNotNull(config, "config should have been initialized");
        return CrawlerManager.fetchBook(input, config);
    }
}
