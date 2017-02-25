package jem.crawler;

import jem.Book;
import jem.Chapter;
import lombok.NonNull;
import lombok.val;

import java.util.concurrent.ExecutorService;

public class CrawlerBook extends Book {
    /**
     * Fetches text of all chapters in executor pool(specified in CrawlerConfig).
     * <p>
     * This method will be blocked until all texts fetched.
     * </p>
     *
     * @param executor the executor service for executing task of fetching text
     */
    public final void initTexts(@NonNull ExecutorService executor) {
        for (val chapter : this) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            fetchTexts(chapter, executor);
        }
    }

    private void fetchTexts(Chapter chapter, ExecutorService executor) {
        val text = chapter.getText();
        if (text instanceof CrawlerText) {
            val ct = (CrawlerText) text;
            if (!ct.isSubmitted()) {
                ct.submitTo(executor);
            }
        }
        for (val sub : chapter) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            fetchTexts(sub, executor);
        }
    }
}
