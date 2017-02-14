package jem.crawler;

import jem.core.Book;
import jem.core.Chapter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import pw.phylame.commons.function.Provider;
import pw.phylame.commons.util.Validate;
import pw.phylame.commons.value.Lazy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CrawlerBook extends Book {
    /**
     * Key for chapter count in book extensions.
     */
    public static final String EXT_CHAPTER_COUNT_KEY = "jem.crawler.chapters";

    /**
     * Fetches text of all chapters.
     * <p>This method will be blocked until all texts fetched.</p>
     *
     * @throws InterruptedException if the current thread is interrupted while waiting for fetching
     */
    public void fetchTexts() throws InterruptedException {
        int count = getExtensions().get(EXT_CHAPTER_COUNT_KEY, Integer.class, -1);
        Validate.check(count >= 0, "No chapter count found in extensions with key '%s'", EXT_CHAPTER_COUNT_KEY);
        val pool = Executors.newFixedThreadPool(Math.max(64, Runtime.getRuntime().availableProcessors() * 16));
        CountDownLatch latch = new CountDownLatch(count);
        for (val chapter : this) {
            fetchTexts(chapter, latch, pool);
        }
        latch.await();
        pool.shutdown();
    }

    private void fetchTexts(Chapter chapter, CountDownLatch latch, ExecutorService pool) {
        if (!chapter.isSection()) {
            val text = chapter.getText();
            if (text instanceof CrawlerText) {
                pool.submit(new FetchTask((CrawlerText) text, latch));
            }
        } else {
            for (val sub : chapter) {
                fetchTexts(chapter, latch, pool);
            }
        }
    }

    @RequiredArgsConstructor
    private static class FetchTask implements Runnable {
        private final CrawlerText text;
        private final CountDownLatch latch;

        @Override
        public void run() {
            text.getText();
            latch.countDown();
        }
    }
}
