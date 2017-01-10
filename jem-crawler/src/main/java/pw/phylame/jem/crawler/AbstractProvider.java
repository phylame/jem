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

import java.io.IOException;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import lombok.val;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.core.Chapter;
import pw.phylame.ycl.io.HttpUtils;
import pw.phylame.ycl.io.IOUtils;
import pw.phylame.ycl.util.CollectionUtils;
import pw.phylame.ycl.util.Function;
import pw.phylame.ycl.util.StringUtils;
import pw.phylame.ycl.util.Validate;

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

    protected final void fetchContentsPaged() throws IOException {
        ensureInitialized();
        for (int i = 2, pages = fetchPage(1); i <= pages; ++i) {
            fetchPage(i);
        }
    }

    protected int fetchPage(int page) throws IOException {
        throw new UnsupportedOperationException("require for implementation");
    }

    @Override
    public final String fetchText(Chapter chapter, String url) {
        ensureInitialized();
        if (config.fetchingListener != null) {
            config.fetchingListener.fetchingText(chapterCount, chapterIndex++, chapter);
        }
        return url.isEmpty() ? StringUtils.EMPTY_TEXT : fetchText(url);
    }

    protected abstract String fetchText(String url);

    protected final Document getSoup(String url) throws IOException {
        return Jsoup.connect(url).timeout(config.timeout).get();
    }

    protected final JSONObject getJson(String url, String encoding) throws IOException {
        val conn = HttpUtils.Request.builder()
                .url(url)
                .method("get")
                .connectTimeout(config.timeout)
                .build()
                .connect();
        return new JSONObject(new JSONTokener(IOUtils.readerFor(conn.getInputStream(), encoding)));
    }

    protected final JSONObject postJson(String url, String encoding) throws IOException {
        val conn = HttpUtils.Request.builder()
                .url(url)
                .method("post")
                .connectTimeout(config.timeout)
                .build()
                .connect();
        return new JSONObject(new JSONTokener(IOUtils.readerFor(conn.getInputStream(), encoding)));
    }

    protected final String joinString(Elements soup, String separator) {
        return StringUtils.join(separator, CollectionUtils.map(soup, new Function<Element, String>() {
            @Override
            public String apply(Element e) {
                return StringUtils.trimmed(e.text());
            }
        }));
    }
}
