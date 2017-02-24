import java.util.concurrent.Executors;

import jem.Attributes;
import jem.Chapter;
import jem.crawler.CrawlerBook;
import jem.crawler.CrawlerConfig;
import jem.crawler.CrawlerListener;
import jem.crawler.CrawlerManager;
import jem.epm.EpmManager;
import jem.epm.util.DebugUtils;
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
    public void textFetching(Chapter chapter, int total, int current) {
        System.out.printf("%d/%d: %s\n", current, total, Attributes.getTitle(chapter));
    }

    public static void main(String[] args) throws Exception {
        System.setProperty(EpmManager.AUTO_LOAD_KEY, "true");
        System.setProperty(CrawlerManager.AUTO_LOAD_KEY, "true");
        val pool = Executors.newFixedThreadPool(32);
        val url = "http://h5.17k.com/book/1080397.html";
        val config = CollectionUtils.<String, Object>mapOf(
                CRAWLER_PREFIX + CrawlerConfig.LISTENER, new Test());
        val book = (CrawlerBook) EpmManager.readBook(url, "crawler", config);
        book.initTexts(pool);
        book.initTexts(pool);
        DebugUtils.makeFile(book, "e:/tmp/b", "pmab", null);
        DebugUtils.makeFile(book, "e:/tmp/b", "epub", null);
        book.cleanup();
        pool.shutdown();
    }

}
