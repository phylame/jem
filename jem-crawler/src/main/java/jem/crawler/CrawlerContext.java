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

import org.jsoup.nodes.Document;

import lombok.Getter;
import lombok.Setter;
import pw.phylame.commons.cache.Cacheable;
import pw.phylame.commons.cache.DirectCache;

@Getter
@Setter
public class CrawlerContext {

    // the URL of attributes page
    private final String url;
    private final CrawlerBook book;
    private final CrawlerConfig config;

    // The cache for chapter text.
    private Cacheable cache;
    private CrawlerListener listener;

    private String tocUrl;

    private Document soup;

    /**
     * The last error.
     */
    private Throwable error;

    public CrawlerContext(String url, CrawlerBook book, CrawlerConfig config) {
        this.url = url;
        this.book = book;
        this.config = config;
        this.listener = config.listener;
        this.cache = config.cache != null ? config.cache : new DirectCache();
    }
}
