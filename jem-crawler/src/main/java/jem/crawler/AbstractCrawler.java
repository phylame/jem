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

import jem.Chapter;
import jem.epm.util.InputCleaner;
import lombok.Getter;
import lombok.val;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import pw.phylame.commons.format.Render;
import pw.phylame.commons.io.HttpUtils;
import pw.phylame.commons.io.IOUtils;
import pw.phylame.commons.io.TextCache;
import pw.phylame.commons.util.StringUtils;
import pw.phylame.commons.util.Validate;

import java.io.IOException;
import java.util.Collection;

public abstract class AbstractCrawler implements Crawler {
    protected Context context;
    protected CrawlerBook book;
    protected CrawlerConfig config;

    private int chapterIndex = 1;

    @Getter
    protected int chapterCount = -1;

    private boolean initialized = false;

    protected final void ensureInitialized() {
        Validate.check(initialized, "provider is not initialized");
    }

    @Override
    public void init(Context context) {
        this.context = context;
        this.context.setError(null);
        book = context.getBook();
        config = context.getConfig();
        CrawlerText.textCache = new TextCache(context.getCache());
        book.registerCleanup(new InputCleaner(CrawlerText.textCache));
        initialized = true;
    }

    @Override
    public final String fetchText(Chapter chapter, final String url) {
        ensureInitialized();
        if (config.crawlerListener != null) {
            if (chapterIndex > chapterCount) {
                chapterIndex = 1;
            }
            config.crawlerListener.fetchingText(chapterCount, chapterIndex++, chapter);
        }
        if (StringUtils.isEmpty(url)) {
            return StringUtils.EMPTY_TEXT;
        }
        try {
            return fetchText(url);
        } catch (IOException e) {
            context.setError(e);
            return StringUtils.EMPTY_TEXT;
        }
    }

    protected abstract String fetchText(String url) throws IOException;

    protected int fetchPage(int page) throws IOException {
        throw new UnsupportedOperationException("require for implementation");
    }

    protected final void fetchTocPaged() throws IOException {
        ensureInitialized();
        for (int i = 2, pages = fetchPage(1); i <= pages; ++i) {
            fetchPage(i);
        }
    }

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

    protected final String joinString(Collection<? extends Node> nodes, String separator) {
        return StringUtils.join(separator, nodes, new Render<Node>() {
            @Override
            public String render(Node node) {
                String text;
                if (node instanceof Element) {
                    text = ((Element) node).text();
                } else if (node instanceof TextNode) {
                    text = ((TextNode) node).text();
                } else {
                    text = node.toString();
                }
                return StringUtils.trimmed(text.replace("\u00a0", ""));
            }
        });
    }
}
