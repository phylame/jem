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

import jem.core.Book;
import jem.core.Chapter;
import jem.util.flob.Flob;
import jem.util.flob.Flobs;
import lombok.SneakyThrows;
import lombok.val;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public abstract class AbstractProvider implements CrawlerProvider {
    protected Book book;
    protected CrawlerConfig config;
    protected CrawlerContext context;

    protected int chapterCount = -1;
    private int chapterIndex = 1;

    // for cache fetched text
    private static final String CACHE_ENCODING = "UTF-16BE";
    private File cacheFile;
    private RandomAccessFile cacheRaf;
    private Map<String, Flob> texts = new ConcurrentHashMap<>();
    private static final ThreadPoolExecutor cacheService = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);

    private boolean initialized = false;

    public static void cleanup() {
        cacheService.shutdown();
    }

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
        // init cache
        try {
            cacheFile = File.createTempFile("jem-crawling", ".tmp");
            cacheRaf = new RandomAccessFile(cacheFile, "rw");
            book.registerCleanup(new FileDeleter(cacheRaf, cacheFile));
        } catch (IOException e) {
            Log.e("AbstractProvider", "cannot create cache file: {0}", e.getMessage());
        }
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
    @SneakyThrows(IOException.class)
    public final String fetchText(Chapter chapter, final String url) {
        ensureInitialized();
        if (config.fetchingListener != null) {
            if (chapterIndex > chapterCount) {
                chapterIndex = 1;
            }
            config.fetchingListener.fetchingText(chapterCount, chapterIndex++, chapter);
        }
        if (StringUtils.isEmpty(url)) {
            return StringUtils.EMPTY_TEXT;
        }
        val flob = texts.get(url); // find in cache
        if (flob != null) {
            return new String(flob.readAll(), CACHE_ENCODING);
        }
        val str = fetchText(url); // fetch from url
        if (StringUtils.isEmpty(str)) {
            return str;
        }
        cacheService.submit(new Runnable() { // executed in other thread
            @Override
            public void run() {
                val flob = cacheText(url, str); // cache to file
                if (flob != null) {
                    texts.put(url, flob);
                }
            }
        });
        return str;
    }

    protected abstract String fetchText(String url);

    @SneakyThrows(IOException.class)
    private Flob cacheText(String url, String str) {
        if (cacheRaf == null) {
            return null;
        }
        val offset = cacheRaf.getFilePointer();
        cacheRaf.write(str.getBytes(CACHE_ENCODING));
        val flob = Flobs.forBlock("tmp.txt", cacheRaf, offset, str.length() * 2, "text/plain");
        return flob;
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
                    text = ((TextNode) node).text().replace("\u00a0", "");
                } else {
                    text = node.toString();
                }
                return StringUtils.trimmed(text);
            }
        });
    }
}
