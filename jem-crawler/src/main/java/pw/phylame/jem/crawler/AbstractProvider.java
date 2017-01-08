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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.core.Chapter;
import pw.phylame.ycl.util.Validate;

import java.io.IOException;

public abstract class AbstractProvider implements CrawlerProvider {
    protected Book book;
    protected CrawlerConfig config;
    protected CrawlerContext context;

    protected int chapterCount = -1;
    private int chapterIndex = 1;

    private boolean initialized = false;

    protected final void ensureInitialized() {
        Validate.check(initialized, "provider is not initialized");
    }

    @Override
    public void init(CrawlerContext context) {
        this.context = context;
        this.context.setError(null);
        book = context.getBook();
        config = context.getConfig();
        initialized = true;
    }

    @Override
    public final String fetchText(Chapter chapter, String url) {
        ensureInitialized();
        if (config.fetchingListener != null) {
            config.fetchingListener.fetching(chapterCount, chapterIndex++, chapter);
        }
        return fetchText(url);
    }

    protected abstract String fetchText(String url);

    protected final Document getSoup(String url) {
        try {
            return Jsoup.connect(url).timeout(config.timeout).get();
        } catch (IOException e) {
            context.setError(e);
            return null;
        }
    }
}
