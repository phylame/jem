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

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import jem.Attributes;
import jem.Chapter;
import jem.util.text.AbstractText;
import jem.util.text.Texts;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.val;
import pw.phylame.commons.util.StringUtils;
import pw.phylame.commons.util.Validate;

public class CrawlerText extends AbstractText implements Runnable {
    /**
     * Tag for text in cache.
     */
    private volatile Object tag;
    private volatile Future<?> future;

    @Setter
    private CountDownLatch latch;

    @Getter
    private volatile boolean isFetched = false;
    @Getter
    private volatile boolean isSubmitted = false;

    @Getter
    private final String uri;
    @Getter
    private final Chapter chapter;
    @Getter
    private final CrawlerProvider crawler;

    public CrawlerText(@NonNull CrawlerProvider crawler, @NonNull Chapter chapter, @NonNull String uri) {
        super(Texts.PLAIN);
        Validate.requireNotNull(crawler.getContext().getCache(), "cache in context cannot be null");
        this.crawler = crawler;
        this.chapter = chapter;
        this.uri = uri;
    }

    public final Future<?> submitTo(@NonNull ExecutorService executor) {
        isSubmitted = true;
        future = executor.submit(this);
        return future;
    }

    @Override
    @SneakyThrows(IOException.class)
    public String getText() {
        if (!isFetched()) {
            if (isSubmitted()) {// already submitted into pool in async mode
                while (!future.isDone() && !isFetched()) { // wait for done
                    if (Thread.currentThread().isInterrupted()) {
                        return StringUtils.EMPTY_TEXT;
                    }
                    Thread.yield();
                }
            } else {
                fetchText();
            }
        }
        return tag == null ? StringUtils.EMPTY_TEXT : crawler.getContext().getCache().get(tag);
    }

    @Override
    public void run() {
        if (!isFetched()) {
            fetchText();
        }
        if (latch != null) {
            latch.countDown();
        }
    }

    @SneakyThrows(IOException.class)
    private void fetchText() {
        val text = crawler.fetchText(chapter, uri);
        Attributes.setWords(chapter, text.length());
        tag = crawler.getContext().getCache().add(text);
        isFetched = true;
    }

}
