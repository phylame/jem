import jem.Attributes;
import jem.Chapter;
import jem.crawler.CrawlerBook;
import jem.crawler.CrawlerConfig;
import jem.crawler.CrawlerContext;
import jem.crawler.CrawlerListener;
import jem.crawler.impl.WWW_MANGG_COM;
import jem.epm.EpmManager;
import jem.epm.util.DebugUtils;
import lombok.val;

import java.util.concurrent.Executors;

public class Test {
    public static void main(String[] args) throws Exception {
        System.setProperty(EpmManager.AUTO_LOAD_KEY, "true");
//        System.setProperty(CrawlerManager.AUTO_LOAD_KEY, "true");
//        val url = "http://www.mangg.com/id7972/";
//        val pool = Executors.newFixedThreadPool(48);
//        CrawlerBook book = (CrawlerBook) EpmManager.readBook(url, "crawler", CollectionUtils.<String, Object>mapOf(
//                "crawler.parse." + CrawlerConfig.CRAWLER_LISTENER, new CrawlerListener() {
//                    @Override
//                    public void attributeFetched(CrawlerBook book) {
//                        DebugUtils.printAttributes(book, true);
//                    }
//
//                    @Override
//                    public void contentsFetched(CrawlerBook book) {
//
//                    }
//
//                    @Override
//                    public void textFetching(Chapter chapter, int total, int current) {
//                        System.out.printf("%d/%d: %s\n", current, total, Attributes.getTitle(chapter));
//                    }
//                },
//                "crawler.parse." + CrawlerConfig.EXECUTOR, pool));
//        book.fetchTexts();
//        DebugUtils.makeFile(book, "D:/tmp/b", "pmab", null);
//        pool.shutdown();
        val impl = new WWW_MANGG_COM();
        val config = new CrawlerConfig();
        config.crawlerListener = new CrawlerListener() {
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
        };
        config.executor = Executors.newFixedThreadPool(48);
        val context = new CrawlerContext("http://www.mangg.com/id6685/", new CrawlerBook(), config);
        impl.init(context);
        val book = context.getBook();
        impl.fetchAttributes();
        config.crawlerListener.attributeFetched(book);
        impl.fetchContents();
        config.crawlerListener.contentsFetched(book);
        book.fetchTexts();
        DebugUtils.makeFile(book, "D:/tmp/b", "pmab", null);
        DebugUtils.makeFile(book, "D:/tmp/b", "epub", null);
        config.executor.shutdown();
    }
}
