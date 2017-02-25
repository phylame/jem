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

import jem.Book;
import jem.crawler.util.M;
import jem.epm.Parser;
import jem.epm.impl.EpmBase;
import jem.epm.util.ParserException;
import jem.util.JemException;
import lombok.val;
import pw.phylame.commons.util.Validate;

import java.io.File;
import java.io.IOException;
import java.net.URL;
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
        val url = new URL(input);
        val host = url.getProtocol() + "://" + url.getHost();
        final CrawlerProvider crawler;
        try {
            crawler = CrawlerManager.crawlerFor(host);
        } catch (IllegalAccessException | ClassNotFoundException | InstantiationException e) {
            throw new ParserException(M.tr("err.unknownHost", host), e);
        }
        if (crawler == null) {
            throw new ParserException(M.tr("err.unknownHost", host));
        }
        val book = new CrawlerBook();
        val listener = config.listener;
        crawler.init(new CrawlerContext(input, book, config));
        crawler.fetchAttributes();
        if (listener != null) {
            listener.attributeFetched(book);
        }
        crawler.fetchContents();
        if (listener != null) {
            listener.contentsFetched(book);
        }
        book.getAttributes().set("source", input);
        return book;
    }
}
