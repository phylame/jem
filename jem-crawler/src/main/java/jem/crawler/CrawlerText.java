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
import lombok.SneakyThrows;
import lombok.val;
import pw.phylame.commons.util.Validate;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class CrawlerText extends AbstractText implements Callable<String> {
    @Getter
    private final String uri;
    @Getter
    private final Chapter chapter;
    @Getter
    private final CrawlerProvider crawler;

    /**
     * Tag for text in cache.
     */
    private volatile Object tag;
    @Getter
    private volatile boolean fetching;

    /**
     * After fetching done, call {@code countDown()} of the latch.
     */
    private CountDownLatch latch;
    private final CrawlerContext context;
    private final ReentrantLock lock = new ReentrantLock();

    public CrawlerText(@NonNull CrawlerProvider crawler, @NonNull Chapter chapter, @NonNull String uri) {
        super(Texts.PLAIN);
        context = crawler.getContext();
        Validate.requireNotNull(context.getCache(), "cache in context cannot be null");
        this.crawler = crawler;
        this.chapter = chapter;
        this.uri = uri;
    }

    /**
     * Submits the text to specified executor for fetching text in async mode.
     * <p><strong>NOTE:</strong>The {@code getText()} method will block when the task is running.</p>
     *
     * @param executor the executor pool
     * @return the future
     */
    public final Future<String> schedule(@NonNull ExecutorService executor, CountDownLatch latch) {
        lock.lock();
        try {
            Validate.check(!fetching, "Task is already submitted in some thread");
            this.latch = latch;
            return executor.submit(this);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SneakyThrows(IOException.class)
    public final String getText() throws CancellationException {
        lock.lock();
        try {
            return getOrFetch();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public final String call() throws Exception {
        return getOrFetch();
    }

    private String getOrFetch() throws IOException {
        while (fetching) { // text is fetched in async mode
            if (Thread.interrupted()) {
                throw new CancellationException("interrupted");
            }
            Thread.yield();
        }
        if (tag == null) {
            return fetchText(); // fetch in current thread
        } else {
            return context.getCache().get(tag); // get from cache
        }
    }

    /**
     * Fetches text and discard cache(if present).
     *
     * @return the text
     */
    @SneakyThrows(IOException.class)
    public final String fetchText() {
        try {
            fetching = true;
            val text = crawler.fetchText(uri);
            Attributes.setWords(chapter, text.length());
            tag = context.getCache().add(text);
            if (latch != null) {
                latch.countDown();
            }
            notifyTextFetched(chapter, context.getProgress().incrementAndGet());
            return text;
        } finally {
            fetching = false;
        }
    }

    private void notifyTextFetched(Chapter chapter, int progress) {
        val listener = context.getListener();
        if (listener != null) {
            listener.textFetched(chapter, context.getBook().getTotalChapters(), progress);
        }
    }
}
