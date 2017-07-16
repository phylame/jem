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

import jem.Book;
import lombok.NonNull;
import lombok.val;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class CrawlerBook extends Book {
    public static final String ATTRIBUTE_SOURCE = "source";

    private List<Future<String>> futures;
    private final WeakReference<CrawlerContext> context;
    final List<CrawlerText> texts = new ArrayList<>(160);

    CrawlerBook(@NonNull CrawlerContext context) {
        this.context = new WeakReference<>(context);
    }

    public final CrawlerContext getContext() {
        return context.get();
    }

    public final int getTotalChapters() {
        return texts.size();
    }

    public final void fetchTexts(@NonNull ExecutorService executor) {
        fetchTexts(executor, 0);
    }

    /**
     * Fetches text of all chapters in executor pool(specified in CrawlerConfig).
     * <p>
     * This method will be blocked until all texts fetched.
     * </p>
     *
     * @param executor the executor service for executing task of fetching text
     * @param from     index of first text for fetching
     * @return list of future
     */
    public final void fetchTexts(@NonNull ExecutorService executor, int from) {
        futures = new LinkedList<>();
        for (int i = from, end = texts.size(); i < end; ++i) {
            futures.add(texts.get(i).schedule(executor, null));
        }
    }

    /**
     * Cancels all tasks for fetching text.
     */
    public final void cancelFetch() {
        if (futures != null) {
            for (val future : futures) {
                if (!future.isDone()) {
                    future.cancel(true);
                }
            }
            futures = null;
        }
    }

    @Override
    public void cleanup() {
        texts.clear();
        cancelFetch();
        super.cleanup();
    }
}
