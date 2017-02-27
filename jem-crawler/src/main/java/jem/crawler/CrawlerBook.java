package jem.crawler;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import jem.Book;
import jem.Chapter;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.val;
import pw.phylame.commons.util.Validate;

public class CrawlerBook extends Book {
    @Getter
    @Setter
    private int totalChapters = 0;

    WeakReference<CrawlerContext> context;

    private CountDownLatch latch;

    void setContext(CrawlerContext context) {
        this.context = new WeakReference<>(context);
    }

    public final CrawlerContext getContext() {
        return context != null ? context.get() : null;
    }

    /**
     * Fetches text of all chapters in executor pool(specified in CrawlerConfig).
     * <p>
     * This method will be blocked until all texts fetched.
     * </p>
     *
     * @param executor
     *            the executor service for executing task of fetching text
     * @param forced
     *            {@literal true} for fetching text, no matter already fetched
     * @return list of future
     */
    public final List<Future<?>> initTexts(@NonNull ExecutorService executor, boolean forced) {
        val futures = new LinkedList<Future<?>>();
        latch = new CountDownLatch(totalChapters);
        for (val chapter : this) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            fetchTexts(chapter, executor, futures, forced);
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

    private void fetchTexts(Chapter chapter, ExecutorService executor, List<Future<?>> futures, boolean forced) {
        val text = chapter.getText();
        if (text instanceof CrawlerText) {
            val ct = (CrawlerText) text;
            if ((forced || !ct.isFetched()) && !ct.isSubmitted()) {
                ct.setLatch(latch);
                futures.add(ct.submitTo(executor));
            }
        }
        for (val sub : chapter) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            fetchTexts(sub, executor, futures, forced);
        }
    }

}
