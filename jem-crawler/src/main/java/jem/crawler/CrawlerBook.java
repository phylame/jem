package jem.crawler;

import jem.Book;
import jem.Chapter;
import lombok.val;

public class CrawlerBook extends Book {
    /**
     * Fetches text of all chapters in executor pool(specified in CrawlerConfig).
     * <p>This method will be blocked until all texts fetched.</p>
     */
    public final void fetchTexts() throws InterruptedException {
        for (val chapter : this) {
            fetchTexts(chapter);
        }
    }

    private void fetchTexts(Chapter chapter) {
        val text = chapter.getText();
        if (text instanceof CrawlerText) {
            val ct = (CrawlerText) text;
            if (!ct.isSubmitted()) {
                ct.submitToPool();
            }
        }
        for (val sub : chapter) {
            fetchTexts(sub);
        }
    }
}
