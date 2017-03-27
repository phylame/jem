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

import jem.crawler.util.M;
import jem.epm.util.ParserException;
import lombok.NonNull;
import lombok.val;
import pw.phylame.commons.util.Implementor;
import pw.phylame.commons.util.MiscUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

public final class CrawlerManager {
    private CrawlerManager() {
    }

    public static final String AUTO_LOAD_KEY = "jem.crawler.autoLoad";

    private static final String REGISTRY_FILE = "META-INF/jem/crawlers.prop";

    private static final Implementor<CrawlerProvider> crawlers = new Implementor<>(CrawlerProvider.class, false);

    public static void register(String host, Class<? extends CrawlerProvider> clazz) {
        crawlers.register(host, clazz);
    }

    public static void register(String host, String path) {
        crawlers.register(host, path);
    }

    public static boolean isRegistered(String host) {
        return crawlers.contains(host);
    }

    public static void unregister(String host) {
        crawlers.remove(host);
    }

    public static Set<String> supportedHosts() {
        return crawlers.names();
    }

    public static CrawlerProvider crawlerFor(String input) throws ParserException {
        final URL url;
        try {
            url = new URL(input);
        } catch (MalformedURLException e) {
            throw new ParserException(M.tr("err.unknownHost", input), e);
        }
        val host = url.getProtocol() + "://" + url.getHost();
        final CrawlerProvider crawler;
        try {
            crawler = crawlers.getInstance(host);
        } catch (IllegalAccessException | ClassNotFoundException | InstantiationException e) {
            throw new ParserException(M.tr("err.unknownHost", host), e);
        }
        if (crawler == null) {
            throw new ParserException(M.tr("err.unknownHost", host));
        }
        return crawler;
    }

    public static CrawlerBook fetchBook(@NonNull String input, @NonNull CrawlerConfig config)
            throws IOException, ParserException {
        val context = new CrawlerContext(input, config);
        val crawler = crawlerFor(input);
        val book = context.getBook();
        crawler.init(context);
        crawler.fetchAttributes();
        crawler.fetchContents();
        return book;
    }

    public static void loadCrawlers() {
        crawlers.load(REGISTRY_FILE, MiscUtils.getContextClassLoader(), null);
    }

    static {
        if (Boolean.getBoolean(AUTO_LOAD_KEY)) {
            loadCrawlers();
        }
    }
}
