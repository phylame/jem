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

import jem.Attributes;
import jem.Chapter;
import jem.util.text.AbstractText;
import jem.util.text.Texts;
import lombok.NonNull;
import lombok.val;
import pw.phylame.commons.io.TextCache;

public class CrawlerText extends AbstractText {
    /**
     * Cache for text.
     * <p>This field must be initialized in crawler init method.</p>
     */
    static TextCache textCache;

    private final String url;
    private final Chapter chapter;
    private final Crawler crawler;

    /**
     * Tag for getting text from cache.
     */
    private Object tag;

    public CrawlerText(@NonNull String url, @NonNull Crawler crawler, Chapter chapter) {
        super(Texts.PLAIN);
        this.url = url;
        this.chapter = chapter;
        this.crawler = crawler;
    }

    @Override
    public String getText() {
        if (tag != null) {
            return textCache.get(tag);
        } else {
            val text = crawler.fetchText(chapter, url);
            Attributes.setWords(chapter, text.length());
            tag = textCache.add(text);
            return text;
        }
    }
}
