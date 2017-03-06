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

    /**
     * After fetching done, call {@code countDown()} of the latch.
     */
    private CountDownLatch latch;
    private Future<String> future;
    private final ReentrantLock lock = new ReentrantLock();

    public CrawlerText(@NonNull CrawlerProvider crawler, @NonNull Chapter chapter, @NonNull String uri) {
        super(Texts.PLAIN);
        Validate.requireNotNull(crawler.getContext().getCache(), "cache in context cannot be null");
        this.crawler = crawler;
        this.chapter = chapter;
        this.uri = uri;
    }

    /**
     * Returns {@literal true} indicating text is fetching, otherwise {@literal false}
     *
     * @return fetching state
     */
    public final boolean isFetched() {
        return tag != null;
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
            if (future != null && !future.isDone()) {
                return future;
            }
            this.latch = latch;
            future = executor.submit(this);
            return future;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SneakyThrows({IOException.class, ExecutionException.class})
    public final String getText() throws CancellationException {
        lock.lock();
        try {
            if (future != null) { // already submitted into pool in async mode
                try {
                    return future.get(); // wait for done
                } catch (InterruptedException e) {
                    throw new CancellationException("interrupted");
                } finally {
                    future = null; // the task is finished
                }
            } else if (tag == null) {
                return fetchText(); // fetch in current thread
            } else {
                return crawler.getContext().getCache().get(tag); // get from cache
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public final String call() throws Exception {
        try {
            return fetchText();
        } finally {
            future = null;
        }
    }

    /**
     * Fetches text and discard cache(if present).
     *
     * @return the text
     */
    @SneakyThrows(IOException.class)
    public final String fetchText() {
        val text = crawler.fetchText(chapter, uri);
        Attributes.setWords(chapter, text.length());
        tag = crawler.getContext().getCache().add(text);
        if (latch != null) {
            latch.countDown();
        }
        return text;
    }
}
