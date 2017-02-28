package jem.crawler;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import jem.Book;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import pw.phylame.commons.util.Validate;

public class CrawlerBook extends Book {

    WeakReference<CrawlerContext> context;

    void setContext(CrawlerContext context) {
        this.context = new WeakReference<>(context);
    }

    public final CrawlerContext getContext() {
        return context != null ? context.get() : null;
    }

    @Getter
    List<CrawlerText> texts = new ArrayList<>(160);

    public final int getTotalChapters() {
        return texts.size();
    }

    private CountDownLatch latch;

    public final List<Future<?>> initTexts(@NonNull ExecutorService executor) {
        return initTexts(executor, 0);
    }

    /**
     * Fetches text of all chapters in executor pool(specified in CrawlerConfig).
     * <p>
     * This method will be blocked until all texts fetched.
     * </p>
     *
     * @param executor
     *            the executor service for executing task of fetching text
     * @param from
     *            index of first text for fetching
     * @return list of future
     */
    public final List<Future<?>> initTexts(@NonNull ExecutorService executor, int from) {
        val futures = new LinkedList<Future<?>>();
        latch = new CountDownLatch(texts.size() - from);
        for (int i = from, end = texts.size(); i < end; ++i) {
            val text = texts.get(i);
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            text.setLatch(latch);
            futures.add(text.submitTo(executor));
        }
        return futures;
    }

    /**
     * Awaits for all text of chapters fetched.
     * <p>
     * Current thread will be blocked until all text fetched.
     *
     * @author PW[<a href="mailto:phylame@163.com">phylame@163.com</a>]
     * @throws InterruptedException
     *             if the current thread is interrupted while waiting
     */
    public final void awaitFetched() throws InterruptedException {
        Validate.requireNotNull(latch, "latch should have been initialized");
        latch.await();
    }

    @Override
    public void cleanup() {
        latch = null;
        texts.clear();
        super.cleanup();
    }
}
