import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jem.Attributes;
import jem.Chapter;
import jem.crawler.CrawlerBook;
import jem.crawler.CrawlerConfig;
import jem.crawler.CrawlerListener;
import jem.crawler.CrawlerListenerAdapter;
import jem.crawler.CrawlerManager;
import jem.epm.EpmManager;
import jem.epm.util.DebugUtils;
import lombok.RequiredArgsConstructor;
import lombok.val;
import pw.phylame.commons.util.CollectionUtils;

public class Test implements CrawlerListener {
    private static final String CRAWLER_PREFIX = "crawler.parse.";

    @Override
    public void attributeFetched(CrawlerBook book) {
        DebugUtils.printAttributes(book, true);
    }

    @Override
    public void contentsFetched(CrawlerBook book) {
    }

    @Override
    public void textFetched(Chapter chapter, int total, int current) {
        System.out.printf("%d/%d: %s\n", current, total, Attributes.getTitle(chapter));
    }

    public static void main(String[] args) throws Exception {
        System.setProperty(EpmManager.AUTO_LOAD_KEY, "true");
        System.setProperty(CrawlerManager.AUTO_LOAD_KEY, "true");
        val pool = Executors.newFixedThreadPool(64);
        val url = "http://www.mangg.com/id53148/";
        val config = CollectionUtils.<String, Object>mapOf(
                CRAWLER_PREFIX + CrawlerConfig.LISTENER, new Test());
        val book = (CrawlerBook) EpmManager.readBook(url, "crawler", config);
        val futures = book.initTexts(pool);
        book.awaitFetched();
        // for (Future<?> future : futures) {
        // future.cancel(true);
        // }
        System.out.println("done");
        book.cleanup();
        pool.shutdown();
    }

    static ExecutorService pool = Executors.newFixedThreadPool(64);

    @RequiredArgsConstructor
    private static class BookTask implements Runnable {
        private final CrawlerBook book;
        private volatile int progress = 0;
        private List<Future<?>> futures;

        private CrawlerListener l = new CrawlerListenerAdapter() {
            @Override
            public void textFetched(Chapter chapter, int total, int current) {
                progress = current;
            }
        };

        @Override
        public void run() {
            futures = book.initTexts(pool, progress);
            try {
                book.awaitFetched();
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
