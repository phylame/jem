package jem.crawler;

import jem.Book;
import lombok.NonNull;
import lombok.val;
import pw.phylame.commons.util.Validate;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class CrawlerBook extends Book {
    public static final String ATTRIBUTE_SOURCE = "source";

    private CountDownLatch latch;
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
        latch = new CountDownLatch(texts.size() - from);
        for (int i = from, end = texts.size(); i < end; ++i) {
            futures.add(texts.get(i).schedule(executor, latch));
        }
    }

    /**
     * Cancels all tasks for fetching text.
     */
    public final void cancelFetch() {
        if (futures != null) {
            for (val future : futures) {
                future.cancel(true);
            }
            futures = null;
        }
    }

    /**
     * Awaits for all text of chapters fetched.
     * <p>
     * Current thread will be blocked until all text fetched.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public final void awaitFetched() throws InterruptedException {
        Validate.requireNotNull(latch, "latch should have been initialized");
        latch.await();
    }

    @Override
    public void cleanup() {
        latch = null;
        texts.clear();
        cancelFetch();
        super.cleanup();
    }
}
