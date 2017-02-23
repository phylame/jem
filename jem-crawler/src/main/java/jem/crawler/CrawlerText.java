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
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import pw.phylame.commons.util.Validate;

import java.util.concurrent.atomic.AtomicBoolean;

public class CrawlerText extends AbstractText implements Runnable {
    /**
     * Tag for text in cache.
     */
    private Object tag;

    /**
     * Directly text cache.
     */
    private String text;

    @Getter
    private final String uri;
    @Getter
    private final Chapter chapter;
    @Getter
    private final CrawlerProvider crawler;
    private final AtomicBoolean isDone = new AtomicBoolean(false);
    private final AtomicBoolean isSubmitted = new AtomicBoolean(false);

    public CrawlerText(@NonNull CrawlerProvider crawler, @NonNull Chapter chapter, @NonNull String uri) {
        super(Texts.PLAIN);
        this.uri = uri;
        this.chapter = chapter;
        this.crawler = crawler;
    }

    private void awaitFetched() {
        while (!isDone.get()) {
            Thread.yield();
        }
    }

    public final void submitToPool() {
        val executor = crawler.getContext().getConfig().executor;
        Validate.requireNotNull(executor, "executor in context must be initialized for async mode");
        isSubmitted.set(true);
        executor.submit(this);
    }

    public final boolean isSubmitted() {
        return isSubmitted.get();
    }

    @Override
    public String getText() {
        val executor = crawler.getContext().getConfig().executor;
        if (executor != null) { // async mode
            if (isSubmitted.get()) {
                awaitFetched();
            } else {
                submitToPool();
                awaitFetched();
            }
        } else {
            fetchText();
        }
        val cache = crawler.getContext().getCache();
        if (cache != null) {
            Validate.requireNotNull(tag, "tag should have been initialized");
            return cache.get(tag);
        } else {
            Validate.requireNotNull(text, "text should have been initialized");
            return text;
        }
    }

    @Override
    public void run() {
        fetchText();
    }

    private void fetchText() {
        val text = crawler.fetchText(chapter, uri);
        Attributes.setWords(chapter, text.length());
        val cache = crawler.getContext().getCache();
        if (cache != null) {
            tag = cache.add(text);
        } else {
            this.text = text;
        }
        isDone.set(true);
    }
}
