package jem.crawler;

import jem.Book;
import jem.Chapter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import pw.phylame.commons.util.Validate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

public class CrawlerBook extends Book {
    /**
     * Key for chapter count in book extensions.
     */
    public static final String EXT_CHAPTER_COUNT_KEY = "jem.crawler.chapters";

    /**
     * Fetches text of all chapters.
     * <p>This method will be blocked until all texts fetched.</p>
     *
     * @param pool the thread pool for executing fetching task
     * @throws InterruptedException if the current thread is interrupted while waiting for fetching
     */
    public void fetchTexts(ExecutorService pool) throws InterruptedException {
        int count = getExtensions().get(EXT_CHAPTER_COUNT_KEY, Integer.class, -1);
        Validate.check(count >= 0, "No chapter count found in extensions with key '%s'", EXT_CHAPTER_COUNT_KEY);
        val latch = new CountDownLatch(count);
        for (val chapter : this) {
            fetchTexts(chapter, latch, pool);
        }
        latch.await();
    }

    private void fetchTexts(Chapter chapter, CountDownLatch latch, ExecutorService pool) {
        if (!chapter.isSection()) {
            val text = chapter.getText();
            if (text instanceof CrawlerText) {
                pool.submit(new FetchTask((CrawlerText) text, latch));
            }
        } else {
            for (val sub : chapter) {
                fetchTexts(sub, latch, pool);
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
