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
import jem.crawler.util.SoupUtils;
import jem.epm.util.InputCleaner;
import lombok.Getter;
import lombok.NonNull;
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
import pw.phylame.commons.log.Log;
import pw.phylame.commons.util.StringUtils;
import pw.phylame.commons.util.Validate;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractCrawler implements CrawlerProvider {
    protected final String TAG = getClass().getSimpleName();

    @Getter
    protected CrawlerContext context;

    protected int chapterCount = -1;
    private final AtomicInteger chapterIndex = new AtomicInteger(1);

    private boolean initialized = false;

    protected final void ensureInitialized() {
        Validate.check(initialized, "provider is not initialized");
    }

    @Override
    public void init(@NonNull CrawlerContext context) {
        this.context = context;
        context.setError(null);
        val config = context.getConfig();
        if (config.cache != null) {
            context.getBook().registerCleanup(new InputCleaner(config.cache));
        }
        initialized = true;
    }

    protected abstract String fetchText(String uri) throws IOException;

    @Override
    public final String fetchText(Chapter chapter, String uri) {
        ensureInitialized();
        val listener = context.getConfig().listener;
        if (listener != null) {
            Validate.require(chapterCount >= 0, "chapterCount should be initialized");
            listener.textFetching(chapter, chapterCount, chapterIndex.getAndIncrement());
        }
        if (StringUtils.isEmpty(uri)) {
            return StringUtils.EMPTY_TEXT;
        }
        try {
            return fetchText(uri);
        } catch (IOException e) {
            Log.e(TAG, e);
            context.setError(e);
            return StringUtils.EMPTY_TEXT;
        }
    }

    protected int fetchPage(int page) throws IOException {
        throw new UnsupportedOperationException("require for implementation");
    }

    protected final void fetchToc() throws IOException, InterruptedException {
        ensureInitialized();
        for (int i = 2, pages = fetchPage(1); i <= pages; ++i) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            fetchPage(i);
        }
    }

    protected final Document getSoup(String url) throws IOException, InterruptedException {
        return fetchSoup(url, "get");
    }

    protected final Document postSoup(String url) throws IOException, InterruptedException {
        return fetchSoup(url, "post");
    }

    protected final Document fetchSoup(String url, String method) throws IOException, InterruptedException {
        val config = context.getConfig();
        for (int i = 0, end = Math.max(1, config.tryCount); i < end; ++i) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            try {
                return "get".equalsIgnoreCase(method)
                        ? Jsoup.connect(url)
                        .userAgent(SoupUtils.randAgent())
                        .timeout(config.timeout).get()
                        : Jsoup.connect(url)
                        .userAgent(SoupUtils.randAgent())
                        .timeout(config.timeout).post();
            } catch (SocketTimeoutException e) {
                Log.d(TAG, "try reconnect to %s", url);
            }
        }
        throw new IOException("cannot connect to " + url);
    }

    protected final JSONObject getJson(String url, String encoding) throws IOException, InterruptedException {
        return fetchJson(url, "get", encoding);
    }

    protected final JSONObject postJson(String url, String encoding) throws IOException, InterruptedException {
        return fetchJson(url, "post", encoding);
    }

    protected final JSONObject fetchJson(String url, String method, String encoding) throws IOException, InterruptedException {
        val config = context.getConfig();
        val request = HttpUtils.Request.builder()
                .url(url)
                .method(method)
                .property("User-Agent", SoupUtils.randAgent())
                .connectTimeout(config.timeout)
                .build();
        for (int i = 0, end = Math.max(1, config.tryCount); i < end; ++i) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            try {
                return new JSONObject(new JSONTokener(IOUtils.readerFor(request.connect().getInputStream(), encoding)));
            } catch (SocketTimeoutException e) {
                Log.d(TAG, "try reconnect to %s", url);
            }
        }
        throw new IOException("cannot connect to " + url);
    }

    protected final String joinNodes(Collection<? extends Node> nodes, String separator) {
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
