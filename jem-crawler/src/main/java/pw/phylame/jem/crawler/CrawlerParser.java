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

package pw.phylame.jem.crawler;

import lombok.val;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.crawler.util.M;
import pw.phylame.jem.epm.Parser;
import pw.phylame.jem.epm.impl.EpmBase;
import pw.phylame.jem.epm.util.ParserException;
import pw.phylame.jem.util.JemException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

public class CrawlerParser extends EpmBase<CrawlerConfig> implements Parser {
    public CrawlerParser() {
        super("crawler", CrawlerConfig.class);
    }

    @Override
    public Book parse(File file, Map<String, Object> args) throws IOException, JemException {
        throw new UnsupportedOperationException("Crawler parser is't supported for file input");
    }

    @Override
    public Book parse(String input, Map<String, Object> args) throws IOException, JemException {
        val config = fetchConfig(args);
        val url = new URL(input);
        val host = url.getProtocol() + "://" + url.getHost();
        final CrawlerProvider provider;
        try {
            provider = ProviderManager.providerForHost(host);
        } catch (IllegalAccessException | ClassNotFoundException | InstantiationException e) {
            throw new ParserException(M.tr("err.unknownHost", host), e);
        }
        if (provider == null) {
            throw new ParserException(M.tr("err.unknownHost", host));
        }
        val context = new CrawlerContext(new Book(), input, config);
        provider.init(context);
        provider.fetchAttributes();
        provider.fetchContents();
        return context.getBook();
    }
}
