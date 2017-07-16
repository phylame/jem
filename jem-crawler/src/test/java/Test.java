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

import jem.Attributes;
import jem.Chapter;
import jem.crawler.CrawlerBook;
import jem.crawler.CrawlerConfig;
import jem.crawler.CrawlerManager;
import jem.crawler.TextFetchListener;
import jem.epm.EpmManager;
import jem.epm.util.DebugUtils;
import jem.util.JemException;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RequiredArgsConstructor
public class Test implements Runnable, TextFetchListener {
    private static final Logger log = LoggerFactory.getLogger(Test.class);
    private static ExecutorService taskPool = Executors.newFixedThreadPool(4);
    private static Queue<ExecutorService> textPools = new LinkedList<>();

    static {
        for (int i = 0; i < 4; i++) {
            textPools.add(Executors.newFixedThreadPool(48));
        }
    }

    private final String url;
    private CrawlerBook book;
    private CrawlerConfig config = new CrawlerConfig();

    private int progress;

    @Override
    public void textFetched(Chapter chapter, int total, int current) {
        log.debug("{}/{}: {}", current, total, Attributes.getTitle(chapter));
        progress = current;
    }

    @Override
    public void run() {
        val start = System.currentTimeMillis();
        if (book == null) {
            log.debug("fetch book info");
            try {
                book = CrawlerManager.fetchBook(url, config);
            } catch (Exception e) {
                log.debug("failed to fetch book", e);
                return;
            }
            DebugUtils.printAttributes(book, true);
        }
        val pool = textPools.poll();
        try {
            book.fetchTexts(pool, progress);
            log.debug("submitted text tasks: {}", pool);
            val output = getOutput();
            EpmManager.writeBook(book, output, "pmab", null);
            log.debug("book fetched: {}, {}", output, System.currentTimeMillis() - start);
        } catch (IOException | JemException e) {
            book.cancelFetch();
            log.debug("make error", e);
        } catch (CancellationException e) {
            book.cancelFetch();
            log.debug("user cancelled", e);
        } catch (Exception e) {
            book.cancelFetch();
            log.debug("unexpected error", e);
        }
        textPools.offer(pool);
    }

    private File getOutput() {
        return new File("D:/tmp/b", Attributes.getTitle(book) + ".pmab");
    }

    private void start() throws InterruptedException {
        config.listener = this;
        Future<?> future = taskPool.submit(this);
//        long millis = 4000;
//        Thread.sleep(millis);
//        log.debug("wait {}, cancel", millis);
//        future.cancel(true);
//        millis = 1000;
//        Thread.sleep(millis);
//        log.debug("wait {}, resubmit", millis);
//        taskPool.submit(this);
    }

    public static void main(String[] args) throws InterruptedException {
        EpmManager.loadImplementors();
        CrawlerManager.loadCrawlers();
        new Test("http://book.qidian.com/info/1003750072").start();
    }
}
